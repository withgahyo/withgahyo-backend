package com.withgahyo.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.withgahyo.domain.auth.repository.RefreshTokenRepository;
import com.withgahyo.domain.user.entity.User;
import com.withgahyo.domain.user.repository.UserRepository;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	private UserService userService;

	@BeforeEach
	void setUp() {
		userService = new UserService(userRepository, refreshTokenRepository);
	}

	@Test
	void withdraw_success_softDeletesUserAndRevokesRefreshTokens() {
		User user = userWithId(User.create("KAKAO", "kakao-1", "같이가효", null), 1L);
		given(userRepository.findById(1L)).willReturn(Optional.of(user));

		userService.withdraw(1L);

		assertThat(user.getDeletedAt()).isNotNull();
		verify(refreshTokenRepository).revokeAllByUserId(eq(1L), any(LocalDateTime.class));
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
