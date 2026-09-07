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
import org.springframework.transaction.annotation.Transactional;

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

	@Transactional(readOnly = true)
	public InternalPlaceBatchGetResponse batchGetPlaces(InternalPlaceBatchGetRequest request) {
		// PlaceResponse.from() 이 LAZY 연관(place.region)을 참조하므로, open-in-view=false
		// 환경에서 응답 매핑이 트랜잭션(=영속성 컨텍스트) 안에서 끝나도록 readOnly 트랜잭션을 연다.
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
