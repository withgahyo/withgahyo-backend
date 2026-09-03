package com.withgahyo.infra.ai.dto;

import com.withgahyo.domain.course.entity.TransportMode;
import java.time.LocalDate;
import java.util.List;

public record AiRecommendationGenerateRequest(
	Long generationId,
	Long courseId,
	TripRequest trip,
	List<ParticipantRequest> participants
) {

	public record TripRequest(
		String areaCode,
		String sigunguCode,
		String regionName,
		LocalDate startDate,
		LocalDate endDate,
		TransportMode transportMode,
		List<String> keywordCodes,
		List<Long> mustVisitPlaceIds,
		String additionalRequest
	) {
	}

	public record ParticipantRequest(
		Long userId,
		String relationship,
		List<String> tourismPreferenceCodes,
		List<String> foodPreferenceCodes,
		ConditionRequest condition
	) {
	}

	public record ConditionRequest(
		Integer maxWalkingMinutes,
		String restNeedLevel,
		String stairBurdenLevel,
		String slopeBurdenLevel,
		List<String> requiredFacilityCodes,
		List<String> dietaryRestrictionCodes
	) {
	}
}
