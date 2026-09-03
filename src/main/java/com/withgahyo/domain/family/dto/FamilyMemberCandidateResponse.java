package com.withgahyo.domain.family.dto;

import com.withgahyo.domain.user.entity.User;

public record FamilyMemberCandidateResponse(
	Long userId,
	String nickname,
	String profileImageUrl,
	String maskedEmail,
	boolean alreadyConnected
) {
	public static FamilyMemberCandidateResponse of(User user, boolean alreadyConnected) {
		return new FamilyMemberCandidateResponse(
			user.getUserId(),
			user.getNickname(),
			user.getProfileImageUrl(),
			maskEmail(user.getEmail()),
			alreadyConnected
		);
	}

	private static String maskEmail(String email) {
		if (email == null || !email.contains("@")) {
			return "";
		}

		String[] parts = email.split("@", 2);
		String localPart = parts[0];
		String visiblePrefix = localPart.length() <= 2 ? localPart : localPart.substring(0, 2);
		return visiblePrefix + "***@" + parts[1];
	}
}
