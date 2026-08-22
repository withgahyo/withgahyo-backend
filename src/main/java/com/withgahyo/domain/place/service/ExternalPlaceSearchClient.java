package com.withgahyo.domain.place.service;

public interface ExternalPlaceSearchClient {

	ExternalPlaceSearchPage search(String areaCode, String sigunguCode, String query, String cursor, int size);
}
