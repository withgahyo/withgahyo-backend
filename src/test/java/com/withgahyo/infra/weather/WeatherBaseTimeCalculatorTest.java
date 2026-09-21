package com.withgahyo.infra.weather;

import static org.assertj.core.api.Assertions.assertThat;

import com.withgahyo.infra.weather.WeatherBaseTimeCalculator.BaseDateTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class WeatherBaseTimeCalculatorTest {

	private static final LocalDate DAY = LocalDate.of(2026, 9, 21);

	@Test
	void resolve_usesLatestPastBaseHour_atOrdinaryTime() {
		// 09:00 -> 08시 발표가 가장 최근이며 10분 버퍼를 이미 넘겼다.
		BaseDateTime result = WeatherBaseTimeCalculator.resolve(LocalDateTime.of(DAY, LocalTime.of(9, 0)));

		assertThat(result.baseDate()).isEqualTo("20260921");
		assertThat(result.baseTime()).isEqualTo("0800");
	}

	@Test
	void resolve_fallsBackToPreviousSlot_rightAfterPublishBeforeDelayPasses() {
		// 08:05 -> 발표 후 10분 버퍼가 아직 안 지나서 08시 데이터를 아직 신뢰할 수 없다 -> 05시 사용.
		BaseDateTime result = WeatherBaseTimeCalculator.resolve(LocalDateTime.of(DAY, LocalTime.of(8, 5)));

		assertThat(result.baseDate()).isEqualTo("20260921");
		assertThat(result.baseTime()).isEqualTo("0500");
	}

	@Test
	void resolve_usesNewBaseHour_oncePublishDelayHasPassed() {
		// 08:15 -> 10분 버퍼가 지나 08시 발표를 사용할 수 있다.
		BaseDateTime result = WeatherBaseTimeCalculator.resolve(LocalDateTime.of(DAY, LocalTime.of(8, 15)));

		assertThat(result.baseDate()).isEqualTo("20260921");
		assertThat(result.baseTime()).isEqualTo("0800");
	}

	@Test
	void resolve_usesPreviousDay2300_aroundMidnight() {
		// 00:30 -> 당일 02시 발표가 아직 없으므로 전날 23시 발표를 사용해야 한다.
		BaseDateTime result = WeatherBaseTimeCalculator.resolve(LocalDateTime.of(DAY, LocalTime.of(0, 30)));

		assertThat(result.baseDate()).isEqualTo("20260920");
		assertThat(result.baseTime()).isEqualTo("2300");
	}

	@Test
	void resolve_usesPreviousDay2300_whenJustAfterMidnightBeforeFirstPublish() {
		// 02:05 -> 02시 발표 직후라 10분 버퍼를 못 넘겨 여전히 전날 23시를 사용해야 한다.
		BaseDateTime result = WeatherBaseTimeCalculator.resolve(LocalDateTime.of(DAY, LocalTime.of(2, 5)));

		assertThat(result.baseDate()).isEqualTo("20260920");
		assertThat(result.baseTime()).isEqualTo("2300");
	}

	@Test
	void resolve_usesToday2300_lateAtNight() {
		// 23:59 -> 당일 23시 발표(+10분 버퍼 경과)를 사용한다.
		BaseDateTime result = WeatherBaseTimeCalculator.resolve(LocalDateTime.of(DAY, LocalTime.of(23, 59)));

		assertThat(result.baseDate()).isEqualTo("20260921");
		assertThat(result.baseTime()).isEqualTo("2300");
	}
}
