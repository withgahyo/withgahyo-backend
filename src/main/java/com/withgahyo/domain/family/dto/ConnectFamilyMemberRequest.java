package com.withgahyo.domain.family.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConnectFamilyMemberRequest(
	@NotNull
	Long familyUserId,

	@NotBlank
	@Size(max = 30)
	String relationship
) {
}
