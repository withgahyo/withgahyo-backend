package com.withgahyo.domain.user.controller;

import com.withgahyo.domain.user.dto.ProfileImageUploadResponse;
import com.withgahyo.domain.user.dto.UpdateUserProfileRequest;
import com.withgahyo.domain.user.dto.UserProfileResponse;
import com.withgahyo.domain.user.service.UserService;
import com.withgahyo.global.exception.BusinessException;
import com.withgahyo.global.exception.code.SecurityErrorCode;
import com.withgahyo.global.response.ApiResponse;
import com.withgahyo.global.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@PatchMapping("/me")
	public ApiResponse<UserProfileResponse> updateProfile(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@Valid @RequestBody UpdateUserProfileRequest request
	) {
		if (authenticatedUser == null) {
			throw new BusinessException(SecurityErrorCode.UNAUTHORIZED);
		}
		return ApiResponse.success(userService.updateProfile(authenticatedUser.userId(), request));
	}

	@PostMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<ProfileImageUploadResponse> uploadProfileImage(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@RequestPart("file") MultipartFile file
	) {
		if (authenticatedUser == null) {
			throw new BusinessException(SecurityErrorCode.UNAUTHORIZED);
		}
		return ApiResponse.success(userService.uploadProfileImage(authenticatedUser.userId(), file));
	}

	@DeleteMapping("/me")
	public ApiResponse<Void> withdraw(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		if (authenticatedUser == null) {
			throw new BusinessException(SecurityErrorCode.UNAUTHORIZED);
		}
		userService.withdraw(authenticatedUser.userId());
		return ApiResponse.ok();
	}
}
