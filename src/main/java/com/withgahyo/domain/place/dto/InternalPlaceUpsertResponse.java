package com.withgahyo.domain.place.dto;

import com.withgahyo.domain.place.entity.Place;
import java.util.List;

public record InternalPlaceUpsertResponse(
	List<PlaceResponse> places
) {
	public static InternalPlaceUpsertResponse from(List<Place> places) {
		return new InternalPlaceUpsertResponse(places.stream().map(PlaceResponse::from).toList());
	}

	public record PlaceResponse(
		Long placeId,
		String source,
		String contentId,
		String contentTypeId,
		String name
	) {
		private static PlaceResponse from(Place place) {
			return new PlaceResponse(
				place.getPlaceId(),
				place.getSource(),
				place.getContentId(),
				place.getContentTypeId(),
				place.getName()
			);
		}
	}
}
