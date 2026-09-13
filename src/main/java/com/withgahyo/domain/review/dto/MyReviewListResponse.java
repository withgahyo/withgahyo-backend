package com.withgahyo.domain.review.dto;

import com.withgahyo.domain.review.entity.Review;
import java.util.List;
import java.util.Map;

public record MyReviewListResponse(
	List<MyReviewItemResponse> reviews
) {

	public static MyReviewListResponse of(List<Review> reviews, Map<Long, List<String>> highlightsByReviewId) {
		return new MyReviewListResponse(
			reviews.stream()
				.map(review -> MyReviewItemResponse.of(
					review,
					highlightsByReviewId.getOrDefault(review.getReviewId(), List.of())
				))
				.toList()
		);
	}
}
