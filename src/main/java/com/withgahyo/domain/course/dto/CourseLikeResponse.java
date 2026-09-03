package com.withgahyo.domain.course.dto;

public record CourseLikeResponse(
	Long courseId,
	boolean liked
) {

	public static CourseLikeResponse of(Long courseId, boolean liked) {
		return new CourseLikeResponse(courseId, liked);
	}
}
