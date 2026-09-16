package com.withgahyo.domain.review.dto;

import com.withgahyo.domain.review.entity.Review;
import java.time.LocalDateTime;
import java.util.List;

public record MyReviewItemResponse(
	Long reviewId,
	PendingReviewItemResponse course,
	Byte rating,
	String comment,
	Byte recommendationScore,
	List<String> highlights,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {

	public static MyReviewItemResponse of(Review review, List<String> highlights) {
		return new MyReviewItemResponse(
			review.getReviewId(),
			PendingReviewItemResponse.from(review.getCourse()),
			review.getRating(),
			review.getComment(),
			review.getRecommendationScore(),
			highlights,
			review.getCreatedAt(),
			review.getUpdatedAt()
		);
	}
}
