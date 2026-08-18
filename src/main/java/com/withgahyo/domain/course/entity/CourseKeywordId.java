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
public class CourseKeywordId implements Serializable {

	@Column(name = "course_id")
	private Long courseId;

	@Column(name = "keyword_id")
	private Long keywordId;
}
