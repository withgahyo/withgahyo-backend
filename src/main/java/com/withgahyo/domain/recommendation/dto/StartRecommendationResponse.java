package com.withgahyo.domain.recommendation.dto;

import com.withgahyo.domain.recommendation.entity.RecommendationJob;
import com.withgahyo.domain.recommendation.entity.RecommendationJobStatus;
import java.time.LocalDateTime;

public record StartRecommendationResponse(
	Long generationId,
	Long courseId,
	RecommendationJobStatus status,
	LocalDateTime requestedAt
) {

	public static StartRecommendationResponse from(RecommendationJob job) {
		return new StartRecommendationResponse(
			job.getRecommendationJobId(),
			job.getCourse().getCourseId(),
			job.getStatus(),
			job.getCreatedAt()
		);
	}
}
