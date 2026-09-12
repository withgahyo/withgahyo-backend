package com.withgahyo.domain.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.withgahyo.domain.user.dto.ProfileImageUploadResponse;
import com.withgahyo.domain.user.dto.UpdateUserProfileRequest;
import com.withgahyo.domain.user.dto.UserProfileResponse;
import com.withgahyo.domain.user.service.UserService;
import com.withgahyo.global.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

	@Mock
	private UserService userService;

	private UserController userController;

	@BeforeEach
	void setUp() {
		userController = new UserController(userService);
	}

	@Test
	void updateProfile_returnsUpdatedProfile() {
		UpdateUserProfileRequest request = new UpdateUserProfileRequest(" 지우 ", "https://example.com/profile.png");
		UserProfileResponse serviceResponse = new UserProfileResponse(
			1L,
			"user@example.com",
			"지우",
			"https://example.com/profile.png"
		);
		given(userService.updateProfile(1L, request)).willReturn(serviceResponse);

		var response = userController.updateProfile(new AuthenticatedUser(1L), request);

		assertThat(response.data()).isEqualTo(serviceResponse);
		verify(userService).updateProfile(1L, request);
	}

	@Test
	void uploadProfileImage_returnsUploadedImageUrl() {
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"profile.png",
			"image/png",
			"image".getBytes()
		);
		ProfileImageUploadResponse serviceResponse = new ProfileImageUploadResponse(
			"/uploads/profile-images/1-profile.png"
		);
		given(userService.uploadProfileImage(1L, file)).willReturn(serviceResponse);

		var response = userController.uploadProfileImage(new AuthenticatedUser(1L), file);

		assertThat(response.data()).isEqualTo(serviceResponse);
		verify(userService).uploadProfileImage(1L, file);
	}

	@Test
	void withdraw_returnsEmptySuccess() {
		var response = userController.withdraw(new AuthenticatedUser(1L));

		assertThat(response.data()).isNull();
		verify(userService).withdraw(1L);
	}
}
