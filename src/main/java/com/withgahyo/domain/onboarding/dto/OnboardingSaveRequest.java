package com.withgahyo.domain.onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

// TODO: 여행 기간/컨디션 허용값이 확정되면 String 필드를 enum 타입 + 정확한 값 검증으로 전환
public record OnboardingSaveRequest(
	@NotBlank @Size(max = 30) String tripDuration,
	@NotBlank @Size(max = 30) String walkingTolerance,
	@NotBlank @Size(max = 30) String restPreference,
	@NotBlank @Size(max = 30) String stairsPreference,
	@NotBlank @Size(max = 30) String slopePreference,
	@NotBlank @Size(max = 30) String spicyPreference,
	@NotNull List<Long> tourismPreferenceIds,
	@NotNull List<Long> foodPreferenceIds
) {
}
