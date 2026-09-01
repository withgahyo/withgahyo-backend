package com.withgahyo.domain.recommendation.dto;

import com.withgahyo.domain.course.entity.CourseStatus;
import java.time.LocalDateTime;

public record SelectRecommendationCandidateResponse(
	Long courseId,
	Long generationId,
	Long selectedCandidateId,
	CourseStatus courseStatus,
	Long albumId,
	LocalDateTime confirmedAt
) {
}
