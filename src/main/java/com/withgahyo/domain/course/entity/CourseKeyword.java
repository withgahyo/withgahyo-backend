package com.withgahyo.domain.course.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "course_keyword")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseKeyword {

	@EmbeddedId
	private CourseKeywordId id;

	@MapsId("courseId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "course_id", nullable = false)
	private Course course;

	@MapsId("keywordId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "keyword_id", nullable = false)
	private CourseInterestKeyword keyword;

	public static CourseKeyword create(Course course, CourseInterestKeyword keyword) {
		CourseKeyword courseKeyword = new CourseKeyword();
		courseKeyword.id = new CourseKeywordId(course.getCourseId(), keyword.getKeywordId());
		courseKeyword.course = course;
		courseKeyword.keyword = keyword;
		return courseKeyword;
	}
}
