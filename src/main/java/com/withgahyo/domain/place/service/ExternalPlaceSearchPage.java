package com.withgahyo.domain.place.service;

import java.util.List;

public record ExternalPlaceSearchPage(
	List<ExternalPlaceSearchResult> places,
	boolean hasNext,
	String nextCursor
) {
	public static ExternalPlaceSearchPage empty() {
		return new ExternalPlaceSearchPage(List.of(), false, null);
	}
}
