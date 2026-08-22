package com.withgahyo.domain.place.dto;

import com.withgahyo.domain.place.entity.Place;
import java.math.BigDecimal;
import java.util.List;

public record PlaceSearchResponse(
	List<PlaceResponse> places,
	boolean hasNext,
	String nextCursor
) {
	public static PlaceSearchResponse of(List<Place> places, boolean hasNext, String nextCursor) {
		return new PlaceSearchResponse(places.stream().map(PlaceResponse::from).toList(), hasNext, nextCursor);
	}

	public record PlaceResponse(
		Long placeId,
		String source,
		String externalPlaceId,
		String name,
		String category,
		String address,
		String areaCode,
		String sigunguCode,
		String imageUrl,
		BigDecimal latitude,
		BigDecimal longitude
	) {
		private static PlaceResponse from(Place place) {
			return new PlaceResponse(
				place.getPlaceId(),
				place.getContentTypeId(),
				place.getContentId(),
				place.getName(),
				place.getCat1(),
				place.getAddress(),
				place.getRegion().getAreaCode(),
				place.getRegion().getSigunguCode(),
				place.getImageUrl(),
				place.getLatitude(),
				place.getLongitude()
			);
		}
	}
}
