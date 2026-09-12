package com.withgahyo.domain.user.dto;

public record UpdateUserProfileRequest(
	String nickname,
	String profileImageUrl
) {

	public boolean hasNoValue() {
		return nickname == null && profileImageUrl == null;
	}

	public String normalizedNickname() {
		return nickname == null ? null : nickname.strip();
	}
}
