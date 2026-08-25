package com.withgahyo.domain.place.service;

import com.withgahyo.domain.place.dto.PlaceSearchResponse;
import com.withgahyo.domain.place.dto.RegionSearchResponse;
import com.withgahyo.domain.place.entity.Place;
import com.withgahyo.domain.place.entity.Region;
import com.withgahyo.domain.place.exception.PlaceErrorCode;
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
	private final ExternalPlaceSearchClient externalPlaceSearchClient;
	private final PlaceUpsertWriter placeUpsertWriter;

	public PlaceSearchService(
		RegionRepository regionRepository,
		ExternalPlaceSearchClient externalPlaceSearchClient,
		PlaceUpsertWriter placeUpsertWriter
	) {
		this.regionRepository = regionRepository;
		this.externalPlaceSearchClient = externalPlaceSearchClient;
		this.placeUpsertWriter = placeUpsertWriter;
	}

	@Transactional(readOnly = true)
	public RegionSearchResponse searchRegions(String query) {
		List<Region> regions = StringUtils.hasText(query)
			? regionRepository.findByNameContaining(query)
			: regionRepository.findAll();
		return RegionSearchResponse.from(regions);
	}

	public PlaceSearchResponse searchPlaces(
		String areaCode,
		String sigunguCode,
		String query,
		String cursor,
		Integer size
	) {
		int requestSize = size == null ? DEFAULT_PLACE_SEARCH_SIZE : size;
		String selectedSigunguCode = StringUtils.hasText(sigunguCode) ? sigunguCode : "0";
		regionRepository.findByAreaCodeAndSigunguCode(areaCode, selectedSigunguCode)
			.orElseThrow(() -> new BusinessException(PlaceErrorCode.REGION_NOT_FOUND));

		ExternalPlaceSearchPage externalPage = externalPlaceSearchClient.search(
			areaCode,
			selectedSigunguCode,
			query,
			cursor,
			requestSize
		);

		List<ExternalPlaceSearchResult> matchedResults = externalPage.places()
			.stream()
			.filter(result -> isInSelectedRegion(result, areaCode, selectedSigunguCode))
			.toList();

		List<Place> places = placeUpsertWriter.upsertAll(matchedResults);

		return PlaceSearchResponse.of(places, externalPage.hasNext(), externalPage.nextCursor());
	}

	private boolean isInSelectedRegion(ExternalPlaceSearchResult result, String areaCode, String sigunguCode) {
		boolean sameArea = areaCode.equals(result.areaCode());
		boolean sameSigungu = "0".equals(sigunguCode) || sigunguCode.equals(result.sigunguCode());
		return sameArea && sameSigungu;
	}
}
