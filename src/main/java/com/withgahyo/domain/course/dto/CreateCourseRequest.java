package com.withgahyo.domain.course.dto;

import com.withgahyo.domain.course.entity.TransportMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CreateCourseRequest(
	@NotBlank
	@Size(min = 2, max = 10)
	String title,

	@NotBlank
	String areaCode,

	@NotBlank
	String sigunguCode,

	@NotNull
	LocalDate startDate,

	@NotNull
	LocalDate endDate,

	List<Long> familyMemberIds,

	List<Long> keywordIds,

	List<Long> mustVisitPlaceIds,

	@NotNull
	TransportMode transportMode
) {
	public List<Long> familyMemberIdsOrEmpty() {
		return familyMemberIds == null ? List.of() : familyMemberIds;
	}

	public List<Long> keywordIdsOrEmpty() {
		return keywordIds == null ? List.of() : keywordIds;
	}

	public List<Long> mustVisitPlaceIdsOrEmpty() {
		return mustVisitPlaceIds == null ? List.of() : mustVisitPlaceIds;
	}
}
