package com.withgahyo.domain.review.dto;

import com.withgahyo.domain.course.entity.Course;
import java.time.format.DateTimeFormatter;

public record PendingReviewItemResponse(
	Long courseId,
	String title,
	String period,
	String imageUrl
) {

	private static final DateTimeFormatter FULL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy. MM. dd");
	private static final DateTimeFormatter MONTH_DAY_FORMATTER = DateTimeFormatter.ofPattern("MM. dd");

	public static PendingReviewItemResponse from(Course course) {
		return new PendingReviewItemResponse(
			course.getCourseId(),
			course.getTitle(),
			formatPeriod(course),
			course.getImageUrl()
		);
	}

	private static String formatPeriod(Course course) {
		return "%s - %s".formatted(
			course.getStartDate().format(FULL_DATE_FORMATTER),
			course.getEndDate().format(MONTH_DAY_FORMATTER)
		);
	}
}
