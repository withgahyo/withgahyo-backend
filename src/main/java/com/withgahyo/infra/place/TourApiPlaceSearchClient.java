package com.withgahyo.infra.place;

import com.withgahyo.domain.place.service.ExternalPlaceSearchPage;
import com.withgahyo.domain.place.service.ExternalPlaceSearchResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
class TourApiPlaceSearchClient implements ProviderPlaceSearchClient {

	private static final String SOURCE = "TOUR_API";

	private final RestClient restClient;
	private final String serviceKey;
	private final String mobileApp;

	TourApiPlaceSearchClient(
		@Value("${external.tour-api.base-url:https://apis.data.go.kr/B551011/KorService2}") String baseUrl,
		@Value("${external.tour-api.service-key:}") String serviceKey,
		@Value("${external.tour-api.mobile-app:withgahyo}") String mobileApp
	) {
		this.restClient = RestClient.builder().baseUrl(baseUrl).build();
		this.serviceKey = serviceKey;
		this.mobileApp = mobileApp;
	}

	@Override
	public ExternalPlaceSearchPage search(String areaCode, String sigunguCode, String query, int pageNo, int size) {
		if (!StringUtils.hasText(serviceKey)) {
			return ExternalPlaceSearchPage.empty();
		}
		try {
			Map<String, Object> response = restClient.get()
				.uri(uriBuilder -> uriBuilder
					.path("/searchKeyword2")
					.queryParam("serviceKey", serviceKey)
					.queryParam("MobileOS", "ETC")
					.queryParam("MobileApp", mobileApp)
					.queryParam("_type", "json")
					.queryParam("keyword", query)
					.queryParam("areaCode", areaCode)
					.queryParam("sigunguCode", sigunguCode)
					.queryParam("pageNo", pageNo)
					.queryParam("numOfRows", size)
					.build())
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});

			return parse(response, pageNo, size);
		} catch (RuntimeException exception) {
			return ExternalPlaceSearchPage.empty();
		}
	}

	private ExternalPlaceSearchPage parse(Map<String, Object> response, int pageNo, int size) {
		Map<String, Object> body = map(map(response, "response"), "body");
		int totalCount = intValue(body.get("totalCount"));
		List<Map<String, Object>> items = items(body);
		List<ExternalPlaceSearchResult> places = items.stream()
			.map(this::toResult)
			.toList();
		boolean hasNext = pageNo * size < totalCount;
		return new ExternalPlaceSearchPage(places, hasNext, hasNext ? String.valueOf(pageNo + 1) : null);
	}

	private ExternalPlaceSearchResult toResult(Map<String, Object> item) {
		return new ExternalPlaceSearchResult(
			SOURCE,
			string(item.get("contentid")),
			string(item.get("title")),
			mapCategory(string(item.get("contenttypeid")), string(item.get("cat1"))),
			firstText(string(item.get("addr1")), string(item.get("addr2"))),
			string(item.get("areacode")),
			string(item.get("sigungucode")),
			string(item.get("firstimage")),
			decimal(item.get("mapy")),
			decimal(item.get("mapx"))
		);
	}

	private String mapCategory(String contentTypeId, String cat1) {
		return switch (contentTypeId) {
			case "12" -> "TOURIST_ATTRACTION";
			case "14" -> "CULTURE";
			case "15" -> "FESTIVAL";
			case "28" -> "ACTIVITY";
			case "32" -> "STAY";
			case "38" -> "SHOPPING";
			case "39" -> "FOOD";
			default -> StringUtils.hasText(cat1) ? cat1 : "ETC";
		};
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> map(Map<String, Object> source, String key) {
		Object value = source == null ? null : source.get(key);
		return value instanceof Map<?, ?> ? (Map<String, Object>) value : Map.of();
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> items(Map<String, Object> body) {
		Object item = map(body, "items").get("item");
		if (item instanceof List<?>) {
			return (List<Map<String, Object>>) item;
		}
		if (item instanceof Map<?, ?> singleItem) {
			return List.of((Map<String, Object>) singleItem);
		}
		return List.of();
	}

	private String string(Object value) {
		return value == null ? null : String.valueOf(value);
	}

	private int intValue(Object value) {
		if (value instanceof Number number) {
			return number.intValue();
		}
		if (value == null) {
			return 0;
		}
		try {
			return Integer.parseInt(String.valueOf(value));
		} catch (NumberFormatException exception) {
			return 0;
		}
	}

	private BigDecimal decimal(Object value) {
		if (value == null || !StringUtils.hasText(String.valueOf(value))) {
			return null;
		}
		return new BigDecimal(String.valueOf(value));
	}

	private String firstText(String first, String second) {
		if (StringUtils.hasText(first) && StringUtils.hasText(second)) {
			return first + " " + second;
		}
		return StringUtils.hasText(first) ? first : second;
	}
}
