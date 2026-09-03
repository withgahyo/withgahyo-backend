package com.withgahyo.domain.place.service;

import com.withgahyo.domain.place.dto.InternalPlaceUpsertRequest;
import com.withgahyo.domain.place.dto.InternalPlaceUpsertResponse;
import com.withgahyo.domain.place.entity.Place;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PlaceInternalService {

	private final PlaceUpsertWriter placeUpsertWriter;

	public PlaceInternalService(PlaceUpsertWriter placeUpsertWriter) {
		this.placeUpsertWriter = placeUpsertWriter;
	}

	public InternalPlaceUpsertResponse upsertPlaces(InternalPlaceUpsertRequest request) {
		List<Place> places = placeUpsertWriter.upsertAll(toExternalPlaceSearchResults(request));
		return InternalPlaceUpsertResponse.from(places);
	}

	private List<ExternalPlaceSearchResult> toExternalPlaceSearchResults(InternalPlaceUpsertRequest request) {
		return request.places()
			.stream()
			.map(place -> new ExternalPlaceSearchResult(
				place.source(),
				place.contentTypeId(),
				place.contentId(),
				place.name(),
				place.category(),
				place.address(),
				place.areaCode(),
				place.sigunguCode(),
				place.imageUrl(),
				place.latitude(),
				place.longitude()
			))
			.toList();
	}
}
