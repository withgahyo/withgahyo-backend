package com.withgahyo.domain.course.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
public class CourseLikeId implements Serializable {

	@Column(name = "user_id")
	private Long userId;

	@Column(name = "course_id")
	private Long courseId;

	public static CourseLikeId of(Long userId, Long courseId) {
		CourseLikeId id = new CourseLikeId();
		id.userId = userId;
		id.courseId = courseId;
		return id;
	}
}
