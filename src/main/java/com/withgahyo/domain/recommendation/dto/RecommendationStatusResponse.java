package com.withgahyo.domain.recommendation.dto;

import com.withgahyo.domain.recommendation.entity.RecommendationJob;
import com.withgahyo.domain.recommendation.entity.RecommendationJobStatus;
import java.time.LocalDateTime;

public record RecommendationStatusResponse(
	Long generationId,
	Long courseId,
	RecommendationJobStatus status,
	String currentStage,
	Integer progress,
	String failureCode,
	String failureMessage,
	Boolean retryable,
	LocalDateTime startedAt,
	LocalDateTime completedAt
) {

	public static RecommendationStatusResponse from(RecommendationJob job) {
		return new RecommendationStatusResponse(
			job.getRecommendationJobId(),
			job.getCourse().getCourseId(),
			job.getStatus(),
			null,
			null,
			job.getStatus() == RecommendationJobStatus.FAILED ? "RECOMMENDATION_FAILED" : null,
			job.getErrorMessage(),
			job.getStatus() == RecommendationJobStatus.FAILED ? Boolean.TRUE : null,
			job.getStartedAt(),
			job.getCompletedAt() != null ? job.getCompletedAt() : job.getFailedAt()
		);
	}
}
