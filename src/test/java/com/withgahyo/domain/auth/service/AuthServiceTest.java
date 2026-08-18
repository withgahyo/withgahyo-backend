package com.withgahyo.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.withgahyo.domain.auth.dto.AuthTokenResponse;
import com.withgahyo.domain.auth.dto.TokenRefreshResponse;
import com.withgahyo.domain.auth.entity.RefreshToken;
import com.withgahyo.domain.auth.oauth.OAuthProvider;
import com.withgahyo.domain.auth.oauth.OAuthUserClient;
import com.withgahyo.domain.auth.oauth.OAuthUserInfo;
import com.withgahyo.domain.auth.repository.RefreshTokenRepository;
import com.withgahyo.domain.auth.token.JwtTokenProvider;
import com.withgahyo.domain.user.entity.User;
import com.withgahyo.domain.user.repository.UserRepository;
import com.withgahyo.global.exception.BusinessException;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private OAuthUserClient kakaoOAuthUserClient;

	@Mock
	private OAuthUserClient googleOAuthUserClient;

	@Mock
	private UserRepository userRepository;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(
			Map.of(
				OAuthProvider.KAKAO, kakaoOAuthUserClient,
				OAuthProvider.GOOGLE, googleOAuthUserClient
			),
			userRepository,
			refreshTokenRepository,
			jwtTokenProvider
		);
	}

	@Test
	void loginWithKakao_success_whenNewUser() {
		OAuthUserInfo oauthUserInfo = new OAuthUserInfo(
			OAuthProvider.KAKAO,
			"kakao-1",
			"같이가효",
			"https://example.com/profile.png"
		);
		User savedUser = userWithId(User.create(
			"KAKAO",
			"kakao-1",
			"같이가효",
			"https://example.com/profile.png"
		), 1L);

		given(kakaoOAuthUserClient.getUserInfo("oauth-token")).willReturn(oauthUserInfo);
		given(userRepository.findByProviderAndProviderUserId("KAKAO", "kakao-1")).willReturn(Optional.empty());
		given(userRepository.save(any(User.class))).willReturn(savedUser);
		given(jwtTokenProvider.createAccessToken(1L)).willReturn("access-token");
		given(jwtTokenProvider.createRefreshToken(1L)).willReturn("refresh-token");
		given(jwtTokenProvider.getAccessTokenExpiresIn()).willReturn(3600L);

		AuthTokenResponse response = authService.loginWithKakao("oauth-token");

		assertThat(response.accessToken()).isEqualTo("access-token");
		assertThat(response.refreshToken()).isEqualTo("refresh-token");
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.expiresIn()).isEqualTo(3600L);
		assertThat(response.isNewUser()).isTrue();
		assertThat(response.user().userId()).isEqualTo(1L);
		assertThat(response.user().nickname()).isEqualTo("같이가효");
		assertThat(response.user().profileImageUrl()).isEqualTo("https://example.com/profile.png");
		assertThat(response.user().onboardingCompleted()).isFalse();

		ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
		assertThat(refreshTokenCaptor.getValue().getToken()).isEqualTo("refresh-token");
	}

	@Test
	void loginWithKakao_success_whenDeletedUserRestored() {
		OAuthUserInfo oauthUserInfo = new OAuthUserInfo(
			OAuthProvider.KAKAO,
			"kakao-1",
			"최신닉네임",
			"https://example.com/latest.png"
		);
		User deletedUser = userWithId(User.create(
			"KAKAO",
			"kakao-1",
			"이전닉네임",
			"https://example.com/old.png"
		), 1L);
		deletedUser.withdraw(LocalDateTime.of(2026, 8, 19, 10, 0));

		given(kakaoOAuthUserClient.getUserInfo("oauth-token")).willReturn(oauthUserInfo);
		given(userRepository.findByProviderAndProviderUserId("KAKAO", "kakao-1")).willReturn(Optional.of(deletedUser));
		given(jwtTokenProvider.createAccessToken(1L)).willReturn("access-token");
		given(jwtTokenProvider.createRefreshToken(1L)).willReturn("refresh-token");
		given(jwtTokenProvider.getAccessTokenExpiresIn()).willReturn(3600L);

		AuthTokenResponse response = authService.loginWithKakao("oauth-token");

		assertThat(response.isNewUser()).isFalse();
		assertThat(response.user().nickname()).isEqualTo("최신닉네임");
		assertThat(response.user().profileImageUrl()).isEqualTo("https://example.com/latest.png");
		assertThat(deletedUser.getDeletedAt()).isNull();
	}

	@Test
	void loginWithGoogle_success_whenExistingUser() {
		OAuthUserInfo oauthUserInfo = new OAuthUserInfo(
			OAuthProvider.GOOGLE,
			"google-1",
			"구글유저",
			"https://example.com/google.png"
		);
		User existingUser = userWithId(User.create(
			"GOOGLE",
			"google-1",
			"이전구글",
			"https://example.com/old-google.png"
		), 2L);

		given(googleOAuthUserClient.getUserInfo("google-oauth-token")).willReturn(oauthUserInfo);
		given(userRepository.findByProviderAndProviderUserId("GOOGLE", "google-1")).willReturn(Optional.of(existingUser));
		given(jwtTokenProvider.createAccessToken(2L)).willReturn("google-access-token");
		given(jwtTokenProvider.createRefreshToken(2L)).willReturn("google-refresh-token");
		given(jwtTokenProvider.getAccessTokenExpiresIn()).willReturn(3600L);

		AuthTokenResponse response = authService.loginWithGoogle("google-oauth-token");

		assertThat(response.accessToken()).isEqualTo("google-access-token");
		assertThat(response.refreshToken()).isEqualTo("google-refresh-token");
		assertThat(response.isNewUser()).isFalse();
		assertThat(response.user().userId()).isEqualTo(2L);
		assertThat(response.user().nickname()).isEqualTo("구글유저");
		assertThat(response.user().profileImageUrl()).isEqualTo("https://example.com/google.png");
	}

	@Test
	void logout_success_revokesAllRefreshTokensByUser() {
		authService.logout(1L);

		verify(refreshTokenRepository).revokeAllByUserId(eq(1L), any(LocalDateTime.class));
	}

	@Test
	void refresh_success_rotatesRefreshToken() {
		User user = userWithId(User.create("KAKAO", "kakao-1", "같이가효", null), 1L);
		RefreshToken storedRefreshToken = RefreshToken.create(
			user,
			"old-refresh-token",
			LocalDateTime.now().plusDays(1)
		);

		given(jwtTokenProvider.getUserIdFromRefreshToken("old-refresh-token")).willReturn(1L);
		given(refreshTokenRepository.findByToken("old-refresh-token")).willReturn(Optional.of(storedRefreshToken));
		given(jwtTokenProvider.createAccessToken(1L)).willReturn("new-access-token");
		given(jwtTokenProvider.createRefreshToken(1L)).willReturn("new-refresh-token");
		given(jwtTokenProvider.getAccessTokenExpiresIn()).willReturn(3600L);

		TokenRefreshResponse response = authService.refresh("old-refresh-token");

		assertThat(response.accessToken()).isEqualTo("new-access-token");
		assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.expiresIn()).isEqualTo(3600L);
		assertThat(storedRefreshToken.getRevokedAt()).isNotNull();

		ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
		assertThat(refreshTokenCaptor.getValue().getToken()).isEqualTo("new-refresh-token");
	}

	@Test
	void refresh_fail_whenRefreshTokenAlreadyRevoked() {
		User user = userWithId(User.create("KAKAO", "kakao-1", "같이가효", null), 1L);
		RefreshToken storedRefreshToken = RefreshToken.create(
			user,
			"revoked-refresh-token",
			LocalDateTime.now().plusDays(1)
		);
		storedRefreshToken.revoke(LocalDateTime.now());

		given(jwtTokenProvider.getUserIdFromRefreshToken("revoked-refresh-token")).willReturn(1L);
		given(refreshTokenRepository.findByToken("revoked-refresh-token")).willReturn(Optional.of(storedRefreshToken));

		assertThatThrownBy(() -> authService.refresh("revoked-refresh-token"))
			.isInstanceOf(BusinessException.class);
	}

	private User userWithId(User user, Long userId) {
		try {
			Field field = User.class.getDeclaredField("userId");
			field.setAccessible(true);
			field.set(user, userId);
			return user;
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
