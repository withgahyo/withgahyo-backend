package com.withgahyo.domain.recommendation.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

public record StartRecommendationRequest(
	Long baseGenerationId,
	List<Long> excludedPlaceIds,
	@Size(max = 300, message = "추가 요청은 300자 이하여야 합니다.")
	String additionalRequest
) {
}
