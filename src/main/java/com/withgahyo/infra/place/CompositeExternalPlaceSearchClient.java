package com.withgahyo.infra.place;

import com.withgahyo.domain.place.service.ExternalPlaceSearchClient;
import com.withgahyo.domain.place.service.ExternalPlaceSearchPage;
import com.withgahyo.domain.place.service.ExternalPlaceSearchResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CompositeExternalPlaceSearchClient implements ExternalPlaceSearchClient {

	private final TourApiPlaceSearchClient tourApiPlaceSearchClient;
	private final KakaoPlaceSearchClient kakaoPlaceSearchClient;

	public CompositeExternalPlaceSearchClient(
		TourApiPlaceSearchClient tourApiPlaceSearchClient,
		KakaoPlaceSearchClient kakaoPlaceSearchClient
	) {
		this.tourApiPlaceSearchClient = tourApiPlaceSearchClient;
		this.kakaoPlaceSearchClient = kakaoPlaceSearchClient;
	}

	@Override
	public ExternalPlaceSearchPage search(String areaCode, String sigunguCode, String query, String cursor, int size) {
		int pageNo = parsePageNo(cursor);
		ExternalPlaceSearchPage tourPage = tourApiPlaceSearchClient.search(areaCode, sigunguCode, query, pageNo, size);
		ExternalPlaceSearchPage kakaoPage = kakaoPlaceSearchClient.search(areaCode, sigunguCode, query, pageNo, size);

		Map<String, ExternalPlaceSearchResult> resultByKey = new LinkedHashMap<>();
		append(resultByKey, tourPage.places());
		append(resultByKey, kakaoPage.places());

		boolean hasNext = tourPage.hasNext() || kakaoPage.hasNext();
		String nextCursor = hasNext ? String.valueOf(pageNo + 1) : null;
		return new ExternalPlaceSearchPage(resultByKey.values().stream().toList(), hasNext, nextCursor);
	}

	private int parsePageNo(String cursor) {
		if (cursor == null || cursor.isBlank()) {
			return 1;
		}
		try {
			return Math.max(1, Integer.parseInt(cursor));
		} catch (NumberFormatException exception) {
			return 1;
		}
	}

	private void append(Map<String, ExternalPlaceSearchResult> resultByKey, List<ExternalPlaceSearchResult> results) {
		results.forEach(result -> resultByKey.putIfAbsent(result.source() + ":" + result.externalPlaceId(), result));
	}
}
