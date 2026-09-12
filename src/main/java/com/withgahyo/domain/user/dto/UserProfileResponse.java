package com.withgahyo.domain.user.dto;

import com.withgahyo.domain.user.entity.User;

public record UserProfileResponse(
	Long userId,
	String email,
	String nickname,
	String profileImageUrl
) {

	public static UserProfileResponse from(User user) {
		return new UserProfileResponse(
			user.getUserId(),
			user.getEmail(),
			user.getNickname(),
			user.getProfileImageUrl()
		);
	}
}
