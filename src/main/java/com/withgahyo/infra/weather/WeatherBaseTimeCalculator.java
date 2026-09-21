package com.withgahyo.infra.weather;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 기상청 단기예보(getVilageFcst)는 하루 8번(02,05,08,11,14,17,20,23시) 발표되며,
 * 발표 직후 몇 분간은 데이터가 완전히 준비되지 않을 수 있다. 주어진 "현재 시각" 기준으로
 * 안전하게 사용할 수 있는 가장 최근 base_date/base_time을 계산하는 순수 함수다.
 * 외부 호출이 없어 임의의 now 값으로 단위 테스트가 가능하다.
 */
final class WeatherBaseTimeCalculator {

	static final ZoneId KST = ZoneId.of("Asia/Seoul");

	// 기상청 단기예보 발표 시각(시 단위, 매일 8회)
	private static final int[] BASE_HOURS = {2, 5, 8, 11, 14, 17, 20, 23};

	// 발표 직후 데이터가 아직 준비되지 않았을 가능성을 감안한 안전 여유 시간.
	private static final long PUBLISH_DELAY_MINUTES = 10;

	private static final DateTimeFormatter BASE_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

	private WeatherBaseTimeCalculator() {
	}

	record BaseDateTime(String baseDate, String baseTime) {
	}

	static BaseDateTime resolve(LocalDateTime nowKst) {
		LocalDateTime adjusted = nowKst.minusMinutes(PUBLISH_DELAY_MINUTES);
		LocalDate date = adjusted.toLocalDate();
		int hour = adjusted.getHour();

		int chosenHour = -1;
		for (int i = BASE_HOURS.length - 1; i >= 0; i--) {
			if (BASE_HOURS[i] <= hour) {
				chosenHour = BASE_HOURS[i];
				break;
			}
		}
		if (chosenHour == -1) {
			// 자정~02:10 사이: 오늘자 발표가 아직 없으므로 전날 23시 발표를 사용한다.
			date = date.minusDays(1);
			chosenHour = BASE_HOURS[BASE_HOURS.length - 1];
		}

		String baseDate = date.format(BASE_DATE_FORMAT);
		String baseTime = String.format("%02d00", chosenHour);
		return new BaseDateTime(baseDate, baseTime);
	}
}
