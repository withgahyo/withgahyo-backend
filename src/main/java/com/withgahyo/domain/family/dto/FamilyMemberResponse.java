package com.withgahyo.domain.family.dto;

import com.withgahyo.domain.family.entity.FamilyRelation;

public record FamilyMemberResponse(
	Long familyMemberId,
	String nickname,
	String relationship,
	String profileImageUrl
) {
	public static FamilyMemberResponse from(FamilyRelation relation) {
		return new FamilyMemberResponse(
			relation.getFamilyUser().getUserId(),
			relation.getFamilyUser().getNickname(),
			relation.getRelationship(),
			relation.getFamilyUser().getProfileImageUrl()
		);
	}
}
