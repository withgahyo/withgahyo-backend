package com.withgahyo.domain.review.dto;

import com.withgahyo.domain.course.entity.Course;
import java.util.List;

public record ReviewFormResponse(
	PendingReviewItemResponse course,
	Byte ratingMin,
	Byte ratingMax,
	Byte recommendationMin,
	Byte recommendationMax,
	List<String> highlightOptions
) {

	private static final List<String> DEFAULT_HIGHLIGHT_OPTIONS = List.of(
		"여행 코스",
		"편의시설",
		"맛집",
		"기억",
		"교통",
		"추천할래요"
	);

	public static ReviewFormResponse from(Course course) {
		return new ReviewFormResponse(
			PendingReviewItemResponse.from(course),
			(byte) 1,
			(byte) 5,
			(byte) 0,
			(byte) 10,
			DEFAULT_HIGHLIGHT_OPTIONS
		);
	}
}
