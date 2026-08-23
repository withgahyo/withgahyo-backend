package com.withgahyo.domain.course.dto;

import com.withgahyo.domain.course.entity.CourseInterestKeyword;
import java.util.List;

public record CourseKeywordSuggestionsResponse(List<CourseKeywordResponse> keywords) {

	public static CourseKeywordSuggestionsResponse from(List<CourseInterestKeyword> keywords) {
		return new CourseKeywordSuggestionsResponse(
			keywords.stream()
				.map(CourseKeywordResponse::from)
				.toList()
		);
	}

	public record CourseKeywordResponse(Long keywordId, String code, String name) {
		private static CourseKeywordResponse from(CourseInterestKeyword keyword) {
			return new CourseKeywordResponse(keyword.getKeywordId(), keyword.getCode(), keyword.getName());
		}
	}
}
