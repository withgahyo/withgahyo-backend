package com.withgahyo.domain.family.dto;

import com.withgahyo.domain.family.entity.FamilyRelation;
import java.util.List;

public record FamilyMembersResponse(List<FamilyMemberResponse> familyMembers) {

	public static FamilyMembersResponse from(List<FamilyRelation> relations) {
		return new FamilyMembersResponse(
			relations.stream()
				.map(FamilyMemberResponse::from)
				.toList()
		);
	}
}
