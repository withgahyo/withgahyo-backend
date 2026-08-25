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
public class CourseMustVisitPlaceId implements Serializable {

	@Column(name = "course_id")
	private Long courseId;

	@Column(name = "place_id")
	private Long placeId;

	public CourseMustVisitPlaceId(Long courseId, Long placeId) {
		this.courseId = courseId;
		this.placeId = placeId;
	}
}
