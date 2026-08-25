package com.withgahyo.domain.auth.dto;

import com.withgahyo.domain.user.entity.User;

public record AuthUserResponse(
	Long userId,
	String nickname,
	String profileImageUrl,
	String email,
	boolean onboardingCompleted
) {

	public static AuthUserResponse from(User user, boolean onboardingCompleted) {
		return new AuthUserResponse(
			user.getUserId(),
			user.getNickname(),
			user.getProfileImageUrl(),
			user.getEmail(),
			onboardingCompleted
		);
	}
}
