package com.withgahyo.infra.weather;

import com.withgahyo.infra.weather.dto.KmaVilageFcstResponse;
import java.util.List;

public interface WeatherClient {

	/**
	 * 주어진 격자 좌표의 가장 최근 발표 단기예보 전체 항목을 조회한다.
	 * base_date/base_time 계산은 구현체 내부에서 처리하므로 호출자는 nx/ny만 알면 된다.
	 * 실패(타임아웃, 4xx/5xx, 잘못된 응답, 기상청 error resultCode)는 {@link WeatherApiException}으로 통일한다.
	 */
	List<KmaVilageFcstResponse.Item> getShortTermForecast(int nx, int ny);
}
