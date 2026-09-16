package com.withgahyo.domain.review.dto;

import com.withgahyo.domain.review.entity.Review;
import java.time.LocalDateTime;
import java.util.List;

public record ReviewResponse(
	Long reviewId,
	Long courseId,
	Byte rating,
	String comment,
	Byte recommendationScore,
	List<String> highlights,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {

	public static ReviewResponse of(Review review, List<String> highlights) {
		return new ReviewResponse(
			review.getReviewId(),
			review.getCourse().getCourseId(),
			review.getRating(),
			review.getComment(),
			review.getRecommendationScore(),
			highlights,
			review.getCreatedAt(),
			review.getUpdatedAt()
		);
	}
}
