package com.withgahyo.domain.place.service;

import com.withgahyo.domain.course.exception.CourseErrorCode;
import com.withgahyo.domain.place.dto.PlaceSearchResponse;
import com.withgahyo.domain.place.dto.RegionSearchResponse;
import com.withgahyo.domain.place.entity.Place;
import com.withgahyo.domain.place.entity.Region;
import com.withgahyo.domain.place.repository.PlaceRepository;
import com.withgahyo.domain.place.repository.RegionRepository;
import com.withgahyo.global.exception.BusinessException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PlaceSearchService {

	private static final int DEFAULT_PLACE_SEARCH_SIZE = 10;

	private final RegionRepository regionRepository;
	private final PlaceRepository placeRepository;
	private final ExternalPlaceSearchClient externalPlaceSearchClient;

	public PlaceSearchService(
		RegionRepository regionRepository,
		PlaceRepository placeRepository,
		ExternalPlaceSearchClient externalPlaceSearchClient
	) {
		this.regionRepository = regionRepository;
		this.placeRepository = placeRepository;
		this.externalPlaceSearchClient = externalPlaceSearchClient;
	}

	@Transactional(readOnly = true)
	public RegionSearchResponse searchRegions(String query) {
		List<Region> regions = StringUtils.hasText(query)
			? regionRepository.findByNameContaining(query)
			: regionRepository.findAll();
		return RegionSearchResponse.from(regions);
	}

	@Transactional
	public PlaceSearchResponse searchPlaces(
		String areaCode,
		String sigunguCode,
		String query,
		String cursor,
		Integer size
	) {
		int requestSize = size == null ? DEFAULT_PLACE_SEARCH_SIZE : size;
		String selectedSigunguCode = StringUtils.hasText(sigunguCode) ? sigunguCode : "0";
		Region selectedRegion = regionRepository.findByAreaCodeAndSigunguCode(areaCode, selectedSigunguCode)
			.orElseThrow(() -> new BusinessException(CourseErrorCode.REGION_NOT_FOUND));

		ExternalPlaceSearchPage externalPage = externalPlaceSearchClient.search(
			areaCode,
			selectedSigunguCode,
			query,
			cursor,
			requestSize
		);

		List<Place> places = externalPage.places()
			.stream()
			.filter(result -> isInSelectedRegion(result, areaCode, selectedSigunguCode))
			.map(result -> saveOrUpdatePlace(result, selectedRegion))
			.toList();

		return PlaceSearchResponse.of(places, externalPage.hasNext(), externalPage.nextCursor());
	}

	private boolean isInSelectedRegion(ExternalPlaceSearchResult result, String areaCode, String sigunguCode) {
		boolean sameArea = areaCode.equals(result.areaCode());
		boolean sameSigungu = "0".equals(sigunguCode) || sigunguCode.equals(result.sigunguCode());
		return sameArea && sameSigungu;
	}

	private Place saveOrUpdatePlace(ExternalPlaceSearchResult result, Region selectedRegion) {
		return placeRepository.findByContentIdAndContentTypeId(result.externalPlaceId(), result.source())
			.map(place -> {
				place.updateExternalInfo(
					result.category(),
					selectedRegion,
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
				result.source(),
				result.category(),
				selectedRegion,
				result.name(),
				result.address(),
				result.latitude(),
				result.longitude(),
				result.imageUrl()
			)));
	}
}
