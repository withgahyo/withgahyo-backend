package com.withgahyo.domain.onboarding.dto;

import jakarta.validation.constraints.Size;

// TODO: 컨디션 항목별 허용값이 확정되면 String 대신 enum 타입 + 정확한 값 검증으로 전환. 현재는 프론트가 화면 label이 아닌 option id 문자열을 전송함
// 모든 항목은 선택 입력이며, 값이 있을 때만 최대 30자 검증을 적용한다.
public record ConditionsRequest(
	@Size(max = 30) String walkingTolerance,
	@Size(max = 30) String restPreference,
	@Size(max = 30) String stairsPreference,
	@Size(max = 30) String slopePreference,
	@Size(max = 30) String spicyPreference
) {
}
