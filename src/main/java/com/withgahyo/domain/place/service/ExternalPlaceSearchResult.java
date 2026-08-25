package com.withgahyo.domain.place.service;

import java.math.BigDecimal;

public record ExternalPlaceSearchResult(
	String source,
	String contentTypeId,
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
}
