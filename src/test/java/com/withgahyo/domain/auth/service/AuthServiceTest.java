package com.withgahyo.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.withgahyo.domain.auth.dto.AuthTokenResponse;
import com.withgahyo.domain.auth.entity.RefreshToken;
import com.withgahyo.domain.auth.oauth.OAuthProvider;
import com.withgahyo.domain.auth.oauth.OAuthUserClient;
import com.withgahyo.domain.auth.oauth.OAuthUserInfo;
import com.withgahyo.domain.auth.repository.RefreshTokenRepository;
import com.withgahyo.domain.auth.token.JwtTokenProvider;
import com.withgahyo.domain.user.entity.User;
import com.withgahyo.domain.user.repository.UserRepository;
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
	private UserRepository userRepository;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(
			Map.of(OAuthProvider.KAKAO, kakaoOAuthUserClient),
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
