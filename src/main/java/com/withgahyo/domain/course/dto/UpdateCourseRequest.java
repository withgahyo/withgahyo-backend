package com.withgahyo.domain.course.dto;

public record UpdateCourseRequest(
	String title
) {

	public boolean hasNoValue() {
		return title == null;
	}

	public String normalizedTitle() {
		return title == null ? null : title.strip();
	}
}
