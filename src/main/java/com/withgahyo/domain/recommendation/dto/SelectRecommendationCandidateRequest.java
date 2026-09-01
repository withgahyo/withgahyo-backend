package com.withgahyo.domain.recommendation.dto;

import jakarta.validation.constraints.NotNull;

public record SelectRecommendationCandidateRequest(
	@NotNull(message = "추천 후보 ID는 필수입니다.")
	Long candidateId
) {
}
