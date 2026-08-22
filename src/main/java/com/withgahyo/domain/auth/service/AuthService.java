package com.withgahyo.domain.auth.service;

import com.withgahyo.domain.auth.dto.AuthTokenResponse;
import com.withgahyo.domain.auth.dto.AuthUserResponse;
import com.withgahyo.domain.auth.dto.TokenRefreshResponse;
import com.withgahyo.domain.auth.entity.RefreshToken;
import com.withgahyo.domain.auth.oauth.OAuthProvider;
import com.withgahyo.domain.auth.oauth.OAuthUserClient;
import com.withgahyo.domain.auth.oauth.OAuthUserInfo;
import com.withgahyo.domain.auth.repository.RefreshTokenRepository;
import com.withgahyo.domain.auth.token.JwtTokenProvider;
import com.withgahyo.domain.user.entity.User;
import com.withgahyo.domain.user.repository.UserOnboardingProfileRepository;
import com.withgahyo.domain.user.repository.UserRepository;
import com.withgahyo.global.exception.BusinessException;
import com.withgahyo.global.exception.code.SecurityErrorCode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final Map<OAuthProvider, OAuthUserClient> oauthUserClients;
	private final UserRepository userRepository;
	private final UserOnboardingProfileRepository userOnboardingProfileRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtTokenProvider jwtTokenProvider;

	@Autowired
	public AuthService(
		List<OAuthUserClient> oauthUserClients,
		UserRepository userRepository,
		UserOnboardingProfileRepository userOnboardingProfileRepository,
		RefreshTokenRepository refreshTokenRepository,
		JwtTokenProvider jwtTokenProvider
	) {
		this(toClientMap(oauthUserClients), userRepository, userOnboardingProfileRepository, refreshTokenRepository,
			jwtTokenProvider);
	}

	public AuthService(
		Map<OAuthProvider, OAuthUserClient> oauthUserClients,
		UserRepository userRepository,
		RefreshTokenRepository refreshTokenRepository,
		JwtTokenProvider jwtTokenProvider
	) {
		this(oauthUserClients, userRepository, null, refreshTokenRepository, jwtTokenProvider);
	}

	private AuthService(
		Map<OAuthProvider, OAuthUserClient> oauthUserClients,
		UserRepository userRepository,
		UserOnboardingProfileRepository userOnboardingProfileRepository,
		RefreshTokenRepository refreshTokenRepository,
		JwtTokenProvider jwtTokenProvider
	) {
		this.oauthUserClients = oauthUserClients;
		this.userRepository = userRepository;
		this.userOnboardingProfileRepository = userOnboardingProfileRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.jwtTokenProvider = jwtTokenProvider;
	}

	@Transactional
	public AuthTokenResponse loginWithKakao(String authorizationCode, String redirectUri) {
		return login(OAuthProvider.KAKAO, authorizationCode, redirectUri);
	}

	@Transactional
	public AuthTokenResponse loginWithGoogle(String authorizationCode, String redirectUri) {
		return login(OAuthProvider.GOOGLE, authorizationCode, redirectUri);
	}

	@Transactional
	public void logout(Long userId) {
		// 로그아웃은 현재 사용자에게 발급된 모든 Refresh Token 세션을 즉시 폐기한다.
		refreshTokenRepository.revokeAllByUserId(userId, LocalDateTime.now());
	}

	@Transactional
	public TokenRefreshResponse refresh(String refreshToken) {
		Long userId = jwtTokenProvider.getUserIdFromRefreshToken(refreshToken);
		RefreshToken storedRefreshToken = refreshTokenRepository.findByToken(refreshToken)
			.orElseThrow(() -> new BusinessException(SecurityErrorCode.INVALID_TOKEN));

		if (!storedRefreshToken.isOwnedBy(userId)) {
			throw new BusinessException(SecurityErrorCode.INVALID_TOKEN);
		}

		LocalDateTime now = LocalDateTime.now();
		// Refresh Token Rotation: revokedAt이 null인 row를 원자적으로 폐기한다.
		// UPDATE의 DB row lock 덕분에 동시 요청 중 하나만 이 조건을 만족하므로,
		// 같은 토큰으로 온 재사용(탈취) 시도를 레이스 없이 탐지할 수 있다.
		int revokedRows = refreshTokenRepository.revokeIfActive(refreshToken, now);
		if (revokedRows == 0) {
			// 만료되었거나 이미 폐기(재사용)된 토큰이 들어오면 남은 세션도 정리해 재사용 가능성을 차단한다.
			refreshTokenRepository.revokeAllByUserId(userId, now);
			throw new BusinessException(SecurityErrorCode.INVALID_TOKEN);
		}
		storedRefreshToken.revoke(now);

		User user = storedRefreshToken.getUser();
		String newAccessToken = jwtTokenProvider.createAccessToken(userId);
		String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);
		refreshTokenRepository.save(RefreshToken.create(user, newRefreshToken, jwtTokenProvider.getRefreshTokenExpiresAt()));

		return TokenRefreshResponse.of(newAccessToken, newRefreshToken, jwtTokenProvider.getAccessTokenExpiresIn());
	}

	private AuthTokenResponse login(OAuthProvider provider, String authorizationCode, String redirectUri) {
		// 프런트가 전달한 인가 코드를 제공자 Access Token으로 교환한 뒤 사용자 정보를 조회한다.
		OAuthUserClient oauthUserClient = oauthUserClients.get(provider);
		if (oauthUserClient == null) {
			throw new BusinessException(SecurityErrorCode.INVALID_TOKEN);
		}

		OAuthUserInfo userInfo = oauthUserClient.getUserInfo(authorizationCode, redirectUri);
		UserLookupResult userLookupResult = findOrCreateUser(userInfo);
		User user = userLookupResult.user();

		String accessToken = jwtTokenProvider.createAccessToken(user.getUserId());
		String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());
		refreshTokenRepository.save(RefreshToken.create(user, refreshToken, jwtTokenProvider.getRefreshTokenExpiresAt()));

		boolean onboardingCompleted = isOnboardingCompleted(user.getUserId());
		return AuthTokenResponse.of(
			accessToken,
			refreshToken,
			jwtTokenProvider.getAccessTokenExpiresIn(),
			userLookupResult.isNewUser(),
			AuthUserResponse.from(user, onboardingCompleted)
		);
	}

	private UserLookupResult findOrCreateUser(OAuthUserInfo userInfo) {
		String provider = userInfo.provider().name();
		return userRepository.findByProviderAndProviderUserId(provider, userInfo.providerUserId())
			.map(user -> {
				// 같은 소셜 계정으로 재가입하면 새 row를 만들지 않고 기존 탈퇴 계정을 복구한다.
				user.restoreOrUpdate(userInfo.email(), userInfo.nickname(), userInfo.profileImageUrl());
				return new UserLookupResult(user, false);
			})
			.orElseGet(() -> createUser(provider, userInfo));
	}

	private UserLookupResult createUser(String provider, OAuthUserInfo userInfo) {
		User user = User.create(
			provider,
			userInfo.providerUserId(),
			userInfo.email(),
			userInfo.nickname(),
			userInfo.profileImageUrl()
		);
		try {
			User savedUser = userRepository.save(user);
			// JPA는 unique 제약 위반이 commit 시점에 터질 수 있으므로,
			// flush로 중복 가입 충돌을 이 try/catch 안에서 확정시킨다.
			userRepository.flush();
			return new UserLookupResult(savedUser, true);
		} catch (DataIntegrityViolationException exception) {
			// 동시에 들어온 같은 소셜 계정의 최초 로그인 요청 중 하나가 먼저 유저를 생성한 경우.
			// unique 제약 위반으로 이 요청은 실패하므로, 방금 생성된 유저를 다시 조회해 정상 로그인으로 처리한다.
			User existingUser = userRepository.findByProviderAndProviderUserId(provider, userInfo.providerUserId())
				.orElseThrow(() -> exception);
			existingUser.restoreOrUpdate(userInfo.email(), userInfo.nickname(), userInfo.profileImageUrl());
			return new UserLookupResult(existingUser, false);
		}
	}

	private boolean isOnboardingCompleted(Long userId) {
		if (userOnboardingProfileRepository == null) {
			return false;
		}
		return userOnboardingProfileRepository.findById(userId)
			.map(profile -> profile.isOnboardingCompleted())
			.orElse(false);
	}

	private static Map<OAuthProvider, OAuthUserClient> toClientMap(List<OAuthUserClient> clients) {
		Map<OAuthProvider, OAuthUserClient> clientMap = new EnumMap<>(OAuthProvider.class);
		for (OAuthUserClient client : clients) {
			clientMap.put(client.getProvider(), client);
		}
		return clientMap;
	}

	private record UserLookupResult(User user, boolean isNewUser) {
	}
}
