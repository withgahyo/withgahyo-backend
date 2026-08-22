package com.withgahyo.domain.course.dto;

import com.withgahyo.domain.course.entity.Course;
import com.withgahyo.domain.course.entity.CourseStatus;
import java.time.LocalDateTime;

public record CreateCourseResponse(
	Long courseId,
	String title,
	CourseStatus status,
	LocalDateTime createdAt
) {
	public static CreateCourseResponse from(Course course) {
		return new CreateCourseResponse(
			course.getCourseId(),
			course.getTitle(),
			course.getStatus(),
			course.getCreatedAt()
		);
	}
}
