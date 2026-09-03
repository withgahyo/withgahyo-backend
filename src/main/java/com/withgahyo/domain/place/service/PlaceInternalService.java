package com.withgahyo.domain.place.service;

import com.withgahyo.domain.place.dto.InternalPlaceBatchGetRequest;
import com.withgahyo.domain.place.dto.InternalPlaceBatchGetResponse;
import com.withgahyo.domain.place.dto.InternalPlaceUpsertRequest;
import com.withgahyo.domain.place.dto.InternalPlaceUpsertResponse;
import com.withgahyo.domain.place.entity.Place;
import com.withgahyo.domain.place.repository.PlaceRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class PlaceInternalService {

	private final PlaceUpsertWriter placeUpsertWriter;
	private final PlaceRepository placeRepository;

	public PlaceInternalService(PlaceUpsertWriter placeUpsertWriter, PlaceRepository placeRepository) {
		this.placeUpsertWriter = placeUpsertWriter;
		this.placeRepository = placeRepository;
	}

	public InternalPlaceUpsertResponse upsertPlaces(InternalPlaceUpsertRequest request) {
		List<Place> places = placeUpsertWriter.upsertAll(toExternalPlaceSearchResults(request));
		return InternalPlaceUpsertResponse.from(places);
	}

	public InternalPlaceBatchGetResponse batchGetPlaces(InternalPlaceBatchGetRequest request) {
		List<Long> distinctPlaceIds = request.placeIds().stream().distinct().toList();
		List<Place> foundPlaces = placeRepository.findAllByPlaceIdIn(distinctPlaceIds);
		Set<Long> foundPlaceIds = foundPlaces.stream().map(Place::getPlaceId).collect(Collectors.toSet());
		List<Long> missingPlaceIds = distinctPlaceIds.stream()
			.filter(placeId -> !foundPlaceIds.contains(placeId))
			.toList();
		return InternalPlaceBatchGetResponse.of(foundPlaces, missingPlaceIds);
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
