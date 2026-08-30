package com.withgahyo.domain.course.dto;

import com.withgahyo.domain.course.entity.Course;

public record CourseConfirmResponse(
	Long courseId,
	boolean confirmed
) {

	public static CourseConfirmResponse from(Course course) {
		return new CourseConfirmResponse(course.getCourseId(), course.getConfirmedAt() != null);
	}
}
