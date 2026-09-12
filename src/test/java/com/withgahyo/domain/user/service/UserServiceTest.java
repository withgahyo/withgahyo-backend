package com.withgahyo.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.withgahyo.domain.auth.repository.RefreshTokenRepository;
import com.withgahyo.domain.user.dto.UpdateUserProfileRequest;
import com.withgahyo.domain.user.exception.UserErrorCode;
import com.withgahyo.domain.user.entity.User;
import com.withgahyo.domain.user.repository.UserRepository;
import com.withgahyo.domain.user.service.ProfileImageStorageService;
import com.withgahyo.global.exception.BusinessException;
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

	@Mock
	private ProfileImageStorageService profileImageStorageService;

	private UserService userService;

	@BeforeEach
	void setUp() {
		userService = new UserService(userRepository, refreshTokenRepository, profileImageStorageService);
	}

	@Test
	void withdraw_success_softDeletesUserAndRevokesRefreshTokens() {
		User user = userWithId(User.create("KAKAO", "kakao-1", "같이가효", null), 1L);
		given(userRepository.findById(1L)).willReturn(Optional.of(user));

		userService.withdraw(1L);

		assertThat(user.getDeletedAt()).isNotNull();
		verify(refreshTokenRepository).revokeAllByUserId(eq(1L), any(LocalDateTime.class));
	}

	@Test
	void updateProfile_success_updatesNicknameAndProfileImageUrl() {
		User user = userWithId(
			User.create("KAKAO", "kakao-1", "user@example.com", "기존이름", null),
			1L
		);
		given(userRepository.findById(1L)).willReturn(Optional.of(user));

		var response = userService.updateProfile(
			1L,
			new UpdateUserProfileRequest(" 지우 ", "https://example.com/profile.png")
		);

		assertThat(response.nickname()).isEqualTo("지우");
		assertThat(response.profileImageUrl()).isEqualTo("https://example.com/profile.png");
		assertThat(user.getNickname()).isEqualTo("지우");
		assertThat(user.getProfileImageUrl()).isEqualTo("https://example.com/profile.png");
	}

	@Test
	void updateProfile_fail_whenRequestHasNoValue() {
		assertThatThrownBy(() -> userService.updateProfile(1L, new UpdateUserProfileRequest(null, null)))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(UserErrorCode.USER_UPDATE_EMPTY);
	}

	@Test
	void updateProfile_fail_whenNicknameIsBlankAfterTrim() {
		assertThatThrownBy(() -> userService.updateProfile(1L, new UpdateUserProfileRequest("   ", null)))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(UserErrorCode.INVALID_NICKNAME);
	}

	@Test
	void uploadProfileImage_success_updatesProfileImageUrl() {
		User user = userWithId(
			User.create("KAKAO", "kakao-1", "user@example.com", "기존이름", null),
			1L
		);
		var file = new org.springframework.mock.web.MockMultipartFile(
			"file",
			"profile.png",
			"image/png",
			"image".getBytes()
		);
		given(userRepository.findById(1L)).willReturn(Optional.of(user));
		given(profileImageStorageService.store(1L, file)).willReturn("/uploads/profile-images/1-profile.png");

		var response = userService.uploadProfileImage(1L, file);

		assertThat(response.profileImageUrl()).isEqualTo("/uploads/profile-images/1-profile.png");
		assertThat(user.getProfileImageUrl()).isEqualTo("/uploads/profile-images/1-profile.png");
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
