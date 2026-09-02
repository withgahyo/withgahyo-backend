package com.withgahyo.domain.place.controller;

import com.withgahyo.domain.place.dto.InternalPlaceUpsertRequest;
import com.withgahyo.domain.place.dto.InternalPlaceUpsertResponse;
import com.withgahyo.domain.place.service.PlaceInternalService;
import com.withgahyo.global.exception.BusinessException;
import com.withgahyo.global.exception.code.SecurityErrorCode;
import com.withgahyo.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal")
public class PlaceInternalController {

	private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

	private final PlaceInternalService placeInternalService;
	private final String internalApiKey;

	public PlaceInternalController(
		PlaceInternalService placeInternalService,
		@Value("${internal.ai.api-key}") String internalApiKey
	) {
		this.placeInternalService = placeInternalService;
		this.internalApiKey = internalApiKey;
	}

	@PostMapping("/places/upsert")
	public ApiResponse<InternalPlaceUpsertResponse> upsertPlaces(
		@RequestHeader(value = INTERNAL_API_KEY_HEADER, required = false) String internalApiKey,
		@RequestBody @Valid InternalPlaceUpsertRequest request
	) {
		validateInternalApiKey(internalApiKey);
		return ApiResponse.success(placeInternalService.upsertPlaces(request));
	}

	private void validateInternalApiKey(String requestInternalApiKey) {
		if (!StringUtils.hasText(internalApiKey) || !internalApiKey.equals(requestInternalApiKey)) {
			throw new BusinessException(SecurityErrorCode.FORBIDDEN);
		}
	}
}
