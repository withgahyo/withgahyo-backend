package com.withgahyo.infra.ai.dto;

import java.math.BigDecimal;
import java.util.List;

public record AiRecommendationGenerateResponse(
	Long generationId,
	Long courseId,
	List<CandidateResponse> candidates
) {

	public record CandidateResponse(
		Integer rank,
		String title,
		String summary,
		String reason,
		BigDecimal score,
		Integer totalEstimatedMinutes,
		List<DayResponse> days,
		List<String> warnings
	) {
	}

	public record DayResponse(
		Integer dayNumber,
		List<ItemResponse> items
	) {
	}

	public record ItemResponse(
		Integer order,
		String name,
		String category,
		String source,
		Long placeId,
		String contentId,
		String address,
		Integer estimatedStayMinutes,
		List<String> accessibilityNotes
	) {
	}
}
