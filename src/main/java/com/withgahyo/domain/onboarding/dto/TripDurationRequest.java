package com.withgahyo.domain.onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// TODO: 여행 기간 허용값이 확정되면 String 대신 enum 타입 + 정확한 값 검증으로 전환
public record TripDurationRequest(
	@NotBlank @Size(max = 30) String tripDuration
) {
}
