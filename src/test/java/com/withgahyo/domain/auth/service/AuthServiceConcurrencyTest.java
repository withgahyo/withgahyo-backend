package com.withgahyo.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * 실제 MySQL 없이, RefreshTokenRepository/UserRepository가 보장해야 하는 원자성(unique 제약,
 * 조건부 UPDATE의 row lock)을 스레드 안전한 인메모리 스텁으로 흉내내어 AuthService의 동시성 처리 로직을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceConcurrencyTest {

	private static final String SECRET = "test-secret-for-auth-tests-minimum-32-bytes-long";
	private static final int THREAD_COUNT = 8;

	@Mock
	private OAuthUserClient kakaoOAuthUserClient;

	@Mock
	private UserRepository userRepository;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Test
	void concurrentLoginWithSameOAuthAccount_createsExactlyOneUser() throws InterruptedException {
		AuthService authService = new AuthService(
			Map.of(OAuthProvider.KAKAO, kakaoOAuthUserClient),
			userRepository,
			refreshTokenRepository,
			new JwtTokenProvider(SECRET, 3600, 1209600)
		);

		OAuthUserInfo userInfo = new OAuthUserInfo(OAuthProvider.KAKAO, "kakao-race", "같이가효", null);
		given(kakaoOAuthUserClient.getUserInfo(anyString(), anyString())).willReturn(userInfo);

		Map<String, User> store = new ConcurrentHashMap<>();
		AtomicLong idSequence = new AtomicLong(1);
		given(userRepository.findByProviderAndProviderUserId(anyString(), anyString())).willAnswer(invocation ->
			Optional.ofNullable(store.get(key(invocation.getArgument(0), invocation.getArgument(1)))));
		given(userRepository.save(any(User.class))).willAnswer(invocation -> {
			User user = invocation.getArgument(0);
			// 실제 DB의 IDENTITY 채번은 row가 unique 인덱스에 커밋되어 보이는 시점엔 이미 끝나 있으므로,
			// 맵에 게시하기 전에 id를 먼저 확정해 안전한 발행(happens-before)을 흉내낸다.
			setUserId(user, idSequence.getAndIncrement());
			User racedWinner = store.putIfAbsent(key(user.getProvider(), user.getProviderUserId()), user);
			if (racedWinner != null) {
				throw new DataIntegrityViolationException("duplicate provider/providerUserId");
			}
			return user;
		});

		List<AuthTokenResponse> results = Collections.synchronizedList(new ArrayList<>());
		List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
		runConcurrently(() -> results.add(authService.loginWithKakao("code", "redirect")), failures);

		assertThat(failures).isEmpty();
		assertThat(results).hasSize(THREAD_COUNT);
		assertThat(store).hasSize(1);
		assertThat(results.stream().map(response -> response.user().userId()).distinct().count()).isEqualTo(1);
		assertThat(results.stream().filter(AuthTokenResponse::isNewUser).count()).isEqualTo(1);
	}

	@Test
	void firstLoginFallsBackToExistingUserWhenDuplicateDetectedOnFlush() {
		AuthService authService = new AuthService(
			Map.of(OAuthProvider.KAKAO, kakaoOAuthUserClient),
			userRepository,
			refreshTokenRepository,
			new JwtTokenProvider(SECRET, 3600, 1209600)
		);

		OAuthUserInfo userInfo = new OAuthUserInfo(OAuthProvider.KAKAO, "kakao-flush-race", "최신닉네임", null);
		User existingUser = User.create("KAKAO", "kakao-flush-race", "기존닉네임", null);
		setUserId(existingUser, 7L);

		given(kakaoOAuthUserClient.getUserInfo("code", "redirect")).willReturn(userInfo);
		given(userRepository.findByProviderAndProviderUserId("KAKAO", "kakao-flush-race"))
			.willReturn(Optional.empty(), Optional.of(existingUser));
		given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
		willThrow(new DataIntegrityViolationException("duplicate provider/providerUserId"))
			.given(userRepository).flush();

		AuthTokenResponse response = authService.loginWithKakao("code", "redirect");

		assertThat(response.isNewUser()).isFalse();
		assertThat(response.user().userId()).isEqualTo(7L);
		assertThat(response.user().nickname()).isEqualTo("최신닉네임");
	}

	@Test
	void concurrentRefreshWithSameToken_exactlyOneSucceeds() throws InterruptedException {
		JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(SECRET, 3600, 1209600);
		AuthService authService = new AuthService(
			Map.of(),
			userRepository,
			refreshTokenRepository,
			jwtTokenProvider
		);

		User user = User.create("KAKAO", "kakao-race", "같이가효", null);
		setUserId(user, 1L);
		String sharedRefreshToken = jwtTokenProvider.createRefreshToken(1L);
		RefreshToken storedRefreshToken = RefreshToken.create(user, sharedRefreshToken, LocalDateTime.now().plusDays(1));

		given(refreshTokenRepository.findByToken(sharedRefreshToken)).willReturn(Optional.of(storedRefreshToken));
		AtomicBoolean revoked = new AtomicBoolean(false);
		// 실제 "UPDATE ... WHERE revokedAt IS NULL"의 DB row lock을 CAS로 흉내낸다: 동시 호출 중 하나만 1을 받는다.
		given(refreshTokenRepository.revokeIfActive(eq(sharedRefreshToken), any(LocalDateTime.class)))
			.willAnswer(invocation -> revoked.compareAndSet(false, true) ? 1 : 0);

		List<TokenRefreshResponse> results = Collections.synchronizedList(new ArrayList<>());
		List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
		runConcurrently(() -> results.add(authService.refresh(sharedRefreshToken)), failures);

		assertThat(results).hasSize(1);
		assertThat(failures).hasSize(THREAD_COUNT - 1);
		assertThat(failures).allMatch(BusinessException.class::isInstance);
	}

	private void runConcurrently(Runnable task, List<Throwable> failures) throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
		CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
		CountDownLatch start = new CountDownLatch(1);

		for (int i = 0; i < THREAD_COUNT; i++) {
			executor.submit(() -> {
				ready.countDown();
				try {
					start.await();
					task.run();
				} catch (Throwable throwable) {
					failures.add(throwable);
				}
			});
		}

		ready.await();
		start.countDown();
		executor.shutdown();
		assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
	}

	private static String key(String provider, String providerUserId) {
		return provider + ":" + providerUserId;
	}

	private static void setUserId(User user, Long id) {
		try {
			Field field = User.class.getDeclaredField("userId");
			field.setAccessible(true);
			field.set(user, id);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
