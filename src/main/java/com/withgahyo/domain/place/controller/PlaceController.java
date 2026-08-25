package com.withgahyo.domain.place.controller;

import com.withgahyo.domain.place.dto.PlaceSearchResponse;
import com.withgahyo.domain.place.dto.RegionSearchResponse;
import com.withgahyo.domain.place.service.PlaceSearchService;
import com.withgahyo.global.response.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class PlaceController {

	private final PlaceSearchService placeSearchService;

	public PlaceController(PlaceSearchService placeSearchService) {
		this.placeSearchService = placeSearchService;
	}

	@GetMapping("/regions/search")
	public ApiResponse<RegionSearchResponse> searchRegions(
		@RequestParam(required = false) String query
	) {
		return ApiResponse.success(placeSearchService.searchRegions(query));
	}

	@GetMapping("/places/search")
	public ApiResponse<PlaceSearchResponse> searchPlaces(
		@RequestParam @NotBlank String areaCode,
		@RequestParam(required = false) String sigunguCode,
		@RequestParam @NotBlank String query,
		@RequestParam(required = false) String cursor,
		@RequestParam(required = false) @Positive Integer size
	) {
		return ApiResponse.success(placeSearchService.searchPlaces(areaCode, sigunguCode, query, cursor, size));
	}
}
