package com.withgahyo.domain.onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// TODO: 컨디션 항목별 허용값이 확정되면 String 대신 enum 타입 + 정확한 값 검증으로 전환
public record ConditionsRequest(
	@NotBlank @Size(max = 30) String walkingTolerance,
	@NotBlank @Size(max = 30) String restPreference,
	@NotBlank @Size(max = 30) String stairsPreference,
	@NotBlank @Size(max = 30) String slopePreference,
	@NotBlank @Size(max = 30) String spicyPreference
) {
}
