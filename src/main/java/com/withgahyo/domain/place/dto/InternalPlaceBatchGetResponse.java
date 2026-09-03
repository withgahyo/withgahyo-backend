package com.withgahyo.domain.place.dto;

import com.withgahyo.domain.place.entity.Place;
import java.math.BigDecimal;
import java.util.List;

public record InternalPlaceBatchGetResponse(
	List<PlaceResponse> places,
	List<Long> missingPlaceIds
) {

	public static InternalPlaceBatchGetResponse of(List<Place> places, List<Long> missingPlaceIds) {
		return new InternalPlaceBatchGetResponse(
			places.stream().map(PlaceResponse::from).toList(),
			missingPlaceIds
		);
	}

	public record PlaceResponse(
		Long placeId,
		String name,
		BigDecimal latitude,
		BigDecimal longitude,
		String source,
		String contentId,
		String contentTypeId,
		String address,
		String areaCode,
		String sigunguCode
	) {
		private static PlaceResponse from(Place place) {
			return new PlaceResponse(
				place.getPlaceId(),
				place.getName(),
				place.getLatitude(),
				place.getLongitude(),
				place.getSource(),
				place.getContentId(),
				place.getContentTypeId(),
				place.getAddress(),
				place.getRegion().getAreaCode(),
				place.getRegion().getSigunguCode()
			);
		}
	}
}
