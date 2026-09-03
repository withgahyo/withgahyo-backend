package com.withgahyo.domain.place.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.List;

public record InternalPlaceUpsertRequest(
	@NotEmpty List<@Valid PlaceRequest> places
) {
	public record PlaceRequest(
		@NotBlank String source,
		@NotBlank String contentId,
		@NotBlank String contentTypeId,
		@NotBlank String category,
		@NotBlank String areaCode,
		@NotBlank String sigunguCode,
		@NotBlank String name,
		String address,
		BigDecimal latitude,
		BigDecimal longitude,
		String imageUrl
	) {
	}
}
