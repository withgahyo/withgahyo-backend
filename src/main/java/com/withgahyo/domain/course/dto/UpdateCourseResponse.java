package com.withgahyo.domain.course.dto;

import com.withgahyo.domain.course.entity.Course;
import java.time.LocalDateTime;

public record UpdateCourseResponse(
	Long courseId,
	String title,
	LocalDateTime updatedAt
) {

	public static UpdateCourseResponse from(Course course) {
		return new UpdateCourseResponse(course.getCourseId(), course.getTitle(), course.getUpdatedAt());
	}
}
