package com.withgahyo.domain.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateReviewRequest(
	@NotNull @Min(1) @Max(5) Byte rating,
	@Size(max = 500) String comment,
	@NotNull @Min(0) @Max(10) Byte recommendationScore,
	List<@Size(max = 50) String> highlights
) {

	public List<String> normalizedHighlights() {
		if (highlights == null) {
			return List.of();
		}
		return highlights.stream()
			.filter(highlight -> highlight != null && !highlight.isBlank())
			.map(String::strip)
			.distinct()
			.toList();
	}
}
