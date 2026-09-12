package com.withgahyo.domain.user.service;

import com.withgahyo.domain.auth.repository.RefreshTokenRepository;
import com.withgahyo.domain.user.dto.ProfileImageUploadResponse;
import com.withgahyo.domain.user.dto.UpdateUserProfileRequest;
import com.withgahyo.domain.user.dto.UserProfileResponse;
import com.withgahyo.domain.user.entity.User;
import com.withgahyo.domain.user.exception.UserErrorCode;
import com.withgahyo.domain.user.repository.UserRepository;
import com.withgahyo.global.exception.BusinessException;
import com.withgahyo.global.exception.code.SecurityErrorCode;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final ProfileImageStorageService profileImageStorageService;

	public UserService(
		UserRepository userRepository,
		RefreshTokenRepository refreshTokenRepository,
		ProfileImageStorageService profileImageStorageService
	) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.profileImageStorageService = profileImageStorageService;
	}

	@Transactional
	public void withdraw(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(SecurityErrorCode.INVALID_TOKEN));

		LocalDateTime withdrawnAt = LocalDateTime.now();
		user.withdraw(withdrawnAt);
		refreshTokenRepository.revokeAllByUserId(userId, withdrawnAt);
	}

	@Transactional
	public UserProfileResponse updateProfile(Long userId, UpdateUserProfileRequest request) {
		if (request.hasNoValue()) {
			throw new BusinessException(UserErrorCode.USER_UPDATE_EMPTY);
		}
		validateNickname(request.normalizedNickname());

		User user = userRepository.findById(userId)
			.filter(activeUser -> activeUser.getDeletedAt() == null)
			.orElseThrow(() -> new BusinessException(SecurityErrorCode.INVALID_TOKEN));
		user.updateProfile(request.normalizedNickname(), request.profileImageUrl());
		return UserProfileResponse.from(user);
	}

	@Transactional
	public ProfileImageUploadResponse uploadProfileImage(Long userId, MultipartFile file) {
		User user = userRepository.findById(userId)
			.filter(activeUser -> activeUser.getDeletedAt() == null)
			.orElseThrow(() -> new BusinessException(SecurityErrorCode.INVALID_TOKEN));

		String profileImageUrl = profileImageStorageService.store(userId, file);
		user.updateProfile(null, profileImageUrl);
		return new ProfileImageUploadResponse(profileImageUrl);
	}

	private void validateNickname(String nickname) {
		if (nickname == null) {
			return;
		}
		if (nickname.length() < 2 || nickname.length() > 20) {
			throw new BusinessException(UserErrorCode.INVALID_NICKNAME);
		}
	}
}
