package com.withgahyo.domain.course.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "course_interest_keyword",
		uniqueConstraints = @UniqueConstraint(name = "uk_course_interest_keyword_code", columnNames = "code")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseInterestKeyword {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "keyword_id")
	private Long keywordId;

	@Column(name = "code", nullable = false, length = 50)
	private String code;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "is_active", nullable = false)
	private boolean active = true;

	public static CourseInterestKeyword create(String code, String name) {
		CourseInterestKeyword keyword = new CourseInterestKeyword();
		keyword.code = code;
		keyword.name = name;
		keyword.active = true;
		return keyword;
	}
}
