package com.withgahyo.domain.review.dto;

import com.withgahyo.domain.course.entity.Course;
import java.util.List;

public record PendingReviewListResponse(
	List<PendingReviewItemResponse> reviews
) {

	public static PendingReviewListResponse from(List<Course> courses) {
		return new PendingReviewListResponse(
			courses.stream()
				.map(PendingReviewItemResponse::from)
				.toList()
		);
	}
}
