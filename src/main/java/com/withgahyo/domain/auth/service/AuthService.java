package com.withgahyo.domain.auth.service;

import com.withgahyo.domain.auth.dto.AuthTokenResponse;
import com.withgahyo.domain.auth.dto.AuthUserResponse;
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
import org.springframework.beans.factory.annotation.Autowired;
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
	public AuthTokenResponse loginWithKakao(String oauthAccessToken) {
		return login(OAuthProvider.KAKAO, oauthAccessToken);
	}

	private AuthTokenResponse login(OAuthProvider provider, String oauthAccessToken) {
		OAuthUserClient oauthUserClient = oauthUserClients.get(provider);
		if (oauthUserClient == null) {
			throw new BusinessException(SecurityErrorCode.INVALID_TOKEN);
		}

		OAuthUserInfo userInfo = oauthUserClient.getUserInfo(oauthAccessToken);
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
				user.restoreOrUpdate(userInfo.nickname(), userInfo.profileImageUrl());
				return new UserLookupResult(user, false);
			})
			.orElseGet(() -> {
				User user = User.create(provider, userInfo.providerUserId(), userInfo.nickname(), userInfo.profileImageUrl());
				return new UserLookupResult(userRepository.save(user), true);
			});
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
