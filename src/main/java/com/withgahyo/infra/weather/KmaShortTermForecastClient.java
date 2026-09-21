package com.withgahyo.infra.weather;

import com.withgahyo.infra.weather.WeatherBaseTimeCalculator.BaseDateTime;
import com.withgahyo.infra.weather.dto.KmaVilageFcstResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class KmaShortTermForecastClient implements WeatherClient {

	private static final String GET_VILAGE_FCST_PATH = "/getVilageFcst";
	private static final int CONNECT_TIMEOUT_MILLIS = 3000;
	private static final int READ_TIMEOUT_MILLIS = 5000;

	private final RestClient restClient;
	private final String serviceKey;

	public KmaShortTermForecastClient(
		@Value("${external.weather.base-url:https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0}") String baseUrl,
		@Value("${external.weather.service-key:}") String serviceKey
	) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
		requestFactory.setReadTimeout(READ_TIMEOUT_MILLIS);
		this.restClient = RestClient.builder()
			.baseUrl(baseUrl)
			.requestFactory(requestFactory)
			.build();
		this.serviceKey = serviceKey;
	}

	@Override
	public List<KmaVilageFcstResponse.Item> getShortTermForecast(int nx, int ny) {
		if (!StringUtils.hasText(serviceKey)) {
			throw new WeatherApiException("날씨 API 키(WEATHER_API_KEY)가 설정되어 있지 않습니다.");
		}

		BaseDateTime baseDateTime = WeatherBaseTimeCalculator.resolve(LocalDateTime.now(WeatherBaseTimeCalculator.KST));

		KmaVilageFcstResponse response;
		try {
			response = restClient.get()
				.uri(uriBuilder -> uriBuilder
					.path(GET_VILAGE_FCST_PATH)
					.queryParam("serviceKey", serviceKey)
					.queryParam("dataType", "JSON")
					.queryParam("numOfRows", 1000)
					.queryParam("pageNo", 1)
					.queryParam("base_date", baseDateTime.baseDate())
					.queryParam("base_time", baseDateTime.baseTime())
					.queryParam("nx", nx)
					.queryParam("ny", ny)
					.build())
				.retrieve()
				.body(KmaVilageFcstResponse.class);
		} catch (RestClientException exception) {
			// 타임아웃, 4xx/5xx, 응답이 JSON이 아닌 경우(서비스키 오류 시 기상청 게이트웨이가
			// XML/plain text 에러를 내려주는 경우 포함)를 전부 여기서 하나로 묶는다.
			throw new WeatherApiException("기상청 단기예보 API 호출에 실패했습니다.", exception);
		}

		if (response == null || response.response() == null || response.response().header() == null) {
			throw new WeatherApiException("기상청 단기예보 응답이 비어 있습니다.");
		}
		KmaVilageFcstResponse.Header header = response.response().header();
		if (!header.isSuccess()) {
			throw new WeatherApiException("기상청 단기예보 조회 실패: " + header.resultCode() + " " + header.resultMsg());
		}

		KmaVilageFcstResponse.Body body = response.response().body();
		if (body == null || body.items() == null || body.items().item() == null) {
			return List.of();
		}
		return body.items().item();
	}
}
