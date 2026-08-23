package com.withgahyo.infra.place;

import com.withgahyo.domain.place.service.ExternalPlaceSearchPage;

interface ProviderPlaceSearchClient {

	ExternalPlaceSearchPage search(String areaCode, String sigunguCode, String query, int pageNo, int size);
}
