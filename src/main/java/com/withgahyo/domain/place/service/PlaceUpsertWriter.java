package com.withgahyo.domain.place.service;

import com.withgahyo.domain.place.entity.Place;
import com.withgahyo.domain.place.entity.Region;
import com.withgahyo.domain.place.exception.PlaceErrorCode;
import com.withgahyo.domain.place.repository.PlaceRepository;
import com.withgahyo.domain.place.repository.RegionRepository;
import com.withgahyo.global.exception.BusinessException;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PlaceUpsertWriter {

	private final RegionRepository regionRepository;
	private final PlaceRepository placeRepository;

	public PlaceUpsertWriter(RegionRepository regionRepository, PlaceRepository placeRepository) {
		this.regionRepository = regionRepository;
		this.placeRepository = placeRepository;
	}

	@Transactional
	public List<Place> upsertAll(List<ExternalPlaceSearchResult> results) {
		return results.stream()
			.map(this::saveOrUpdatePlace)
			.toList();
	}

	private Place saveOrUpdatePlace(ExternalPlaceSearchResult result) {
		Region placeRegion = regionRepository.findByAreaCodeAndSigunguCode(result.areaCode(), result.sigunguCode())
			.orElseThrow(() -> new BusinessException(PlaceErrorCode.REGION_NOT_FOUND));

		return placeRepository.findByContentIdAndContentTypeId(result.externalPlaceId(), result.contentTypeId())
			.map(place -> {
				place.updateExternalInfo(
					result.category(),
					placeRegion,
					result.name(),
					result.address(),
					result.latitude(),
					result.longitude(),
					result.imageUrl()
				);
				return place;
			})
			.orElseGet(() -> placeRepository.save(Place.create(
				result.externalPlaceId(),
				result.contentTypeId(),
				result.source(),
				result.category(),
				placeRegion,
				result.name(),
				result.address(),
				result.latitude(),
				result.longitude(),
				result.imageUrl()
			)));
	}
}
