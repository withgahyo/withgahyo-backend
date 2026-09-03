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
class KakaoPlaceSearchClient implements ProviderPlaceSearchClient {

	private static final String SOURCE = "KAKAO";

	private final RestClient restClient;
	private final String restApiKey;

	KakaoPlaceSearchClient(
		@Value("${external.kakao.local-api-base-url:https://dapi.kakao.com}") String baseUrl,
		@Value("${external.kakao.rest-api-key:}") String restApiKey
	) {
		this.restClient = RestClient.builder().baseUrl(baseUrl).build();
		this.restApiKey = restApiKey;
	}

	@Override
	public ExternalPlaceSearchPage search(String areaCode, String sigunguCode, String query, int pageNo, int size) {
		if (!StringUtils.hasText(restApiKey)) {
			return ExternalPlaceSearchPage.empty();
		}
		try {
			Map<String, Object> response = restClient.get()
				.uri(uriBuilder -> uriBuilder
					.path("/v2/local/search/keyword.json")
					.queryParam("query", query)
					.queryParam("page", pageNo)
					.queryParam("size", size)
					.build())
				.header("Authorization", "KakaoAK " + restApiKey)
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});

			return parse(response, areaCode, sigunguCode, pageNo);
		} catch (RuntimeException exception) {
			return ExternalPlaceSearchPage.empty();
		}
	}

	private ExternalPlaceSearchPage parse(Map<String, Object> response, String areaCode, String sigunguCode, int pageNo) {
		List<ExternalPlaceSearchResult> places = documents(response).stream()
			.map(this::toResult)
			.toList();
		boolean hasNext = !booleanValue(map(response, "meta").get("is_end"));
		return new ExternalPlaceSearchPage(places, hasNext, hasNext ? String.valueOf(pageNo + 1) : null);
	}

	private ExternalPlaceSearchResult toResult(Map<String, Object> document) {
		String address = firstText(string(document.get("road_address_name")), string(document.get("address_name")));
		String categoryGroupCode = string(document.get("category_group_code"));
		return new ExternalPlaceSearchResult(
			SOURCE,
			categoryGroupCode,
			string(document.get("id")),
			string(document.get("place_name")),
			mapCategory(categoryGroupCode),
			address,
			KakaoRegionCodeMapper.areaCodeFromAddress(address),
			"0",
			null,
			decimal(document.get("y")),
			decimal(document.get("x"))
		);
	}

	private String mapCategory(String categoryGroupCode) {
		return switch (categoryGroupCode) {
			case "AT4" -> "TOURIST_ATTRACTION";
			case "AD5" -> "STAY";
			case "FD6", "CE7" -> "FOOD";
			case "CT1" -> "CULTURE";
			default -> "ETC";
		};
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> map(Map<String, Object> source, String key) {
		Object value = source == null ? null : source.get(key);
		return value instanceof Map<?, ?> ? (Map<String, Object>) value : Map.of();
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> documents(Map<String, Object> response) {
		Object documents = response == null ? null : response.get("documents");
		return documents instanceof List<?> ? (List<Map<String, Object>>) documents : List.of();
	}

	private String string(Object value) {
		return value == null ? null : String.valueOf(value);
	}

	private BigDecimal decimal(Object value) {
		if (value == null || !StringUtils.hasText(String.valueOf(value))) {
			return null;
		}
		return new BigDecimal(String.valueOf(value));
	}

	private boolean booleanValue(Object value) {
		return value instanceof Boolean booleanValue && booleanValue;
	}

	private String firstText(String first, String second) {
		return StringUtils.hasText(first) ? first : second;
	}
}
