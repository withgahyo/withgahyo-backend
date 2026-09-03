package com.withgahyo.domain.onboarding.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

// TODO: 컨디션 허용값이 확정되면 String 필드를 enum 타입 + 정확한 값 검증으로 전환. 현재는 프론트가 화면 label이 아닌 option id 문자열을 전송함
public record OnboardingSaveRequest(
	@Size(max = 30) String walkingTolerance,
	@Size(max = 30) String restPreference,
	@Size(max = 30) String stairsPreference,
	@Size(max = 30) String slopePreference,
	@Size(max = 30) String spicyPreference,
	@NotNull List<Long> tourismPreferenceIds,
	@NotNull List<Long> foodPreferenceIds
) {
}
