package com.withgahyo.domain.course.dto;

import com.withgahyo.domain.family.entity.FamilyRelation;
import java.util.List;

public record CourseFamilyMembersResponse(List<CourseFamilyMemberResponse> familyMembers) {

	public static CourseFamilyMembersResponse from(List<FamilyRelation> relations) {
		return new CourseFamilyMembersResponse(
			relations.stream()
				.map(CourseFamilyMemberResponse::from)
				.toList()
		);
	}

	public record CourseFamilyMemberResponse(
		Long familyMemberId,
		String nickname,
		String relationship,
		String profileImageUrl
	) {
		private static CourseFamilyMemberResponse from(FamilyRelation relation) {
			return new CourseFamilyMemberResponse(
				relation.getFamilyUser().getUserId(),
				relation.getFamilyUser().getNickname(),
				relation.getRelationship(),
				relation.getFamilyUser().getProfileImageUrl()
			);
		}
	}
}
