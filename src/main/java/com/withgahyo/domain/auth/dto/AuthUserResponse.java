package com.withgahyo.domain.auth.dto;

import com.withgahyo.domain.user.entity.User;

public record AuthUserResponse(
	Long userId,
	String email,
	String nickname,
	String profileImageUrl,
	boolean onboardingCompleted
) {

	public static AuthUserResponse from(User user, boolean onboardingCompleted) {
		return new AuthUserResponse(
			user.getUserId(),
			user.getEmail(),
			user.getNickname(),
			user.getProfileImageUrl(),
			onboardingCompleted
		);
	}
}
