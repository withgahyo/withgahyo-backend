package com.withgahyo.domain.weather.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.withgahyo.domain.weather.dto.UpcomingWeatherResponse;
import com.withgahyo.domain.weather.dto.WeatherCondition;
import com.withgahyo.infra.weather.dto.KmaVilageFcstResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class DailyForecastAssemblerTest {

	@Test
	void assemble_computesMinMaxTemperature_fromHourlyTmp_whenTmnTmxAbsent() {
		List<KmaVilageFcstResponse.Item> items = List.of(
			item("TMP", "20260921", "0600", "15"),
			item("TMP", "20260921", "1200", "24"),
			item("TMP", "20260921", "1800", "19")
		);

		List<UpcomingWeatherResponse.DailyForecastResponse> result =
			DailyForecastAssembler.assemble(items, LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 21));

		assertThat(result).hasSize(1);
		assertThat(result.get(0).minTemperature()).isEqualTo(15);
		assertThat(result.get(0).maxTemperature()).isEqualTo(24);
	}

	@Test
	void assemble_prefersTmnTmx_overHourlyTmpRange() {
		List<KmaVilageFcstResponse.Item> items = List.of(
			item("TMN", "20260921", "0600", "14"),
			item("TMX", "20260921", "1500", "26"),
			item("TMP", "20260921", "0900", "20") // TMN/TMX가 있으면 이 값 범위는 무시되어야 함
		);

		List<UpcomingWeatherResponse.DailyForecastResponse> result =
			DailyForecastAssembler.assemble(items, LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 21));

		assertThat(result.get(0).minTemperature()).isEqualTo(14);
		assertThat(result.get(0).maxTemperature()).isEqualTo(26);
	}

	@Test
	void assemble_usesMaxPop_asPrecipitationProbability() {
		List<KmaVilageFcstResponse.Item> items = List.of(
			item("POP", "20260921", "0600", "20"),
			item("POP", "20260921", "1200", "70"),
			item("POP", "20260921", "1800", "40")
		);

		List<UpcomingWeatherResponse.DailyForecastResponse> result =
			DailyForecastAssembler.assemble(items, LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 21));

		assertThat(result.get(0).precipitationProbability()).isEqualTo(70);
	}

	@Test
	void assemble_usesSky_whenNoPtyIndicatesRain() {
		List<KmaVilageFcstResponse.Item> items = List.of(
			item("PTY", "20260921", "0900", "0"),
			item("SKY", "20260921", "0900", "3"),
			item("SKY", "20260921", "1200", "1"), // 정오와 가장 가까운 값이 채택돼야 함
			item("SKY", "20260921", "1800", "4")
		);

		List<UpcomingWeatherResponse.DailyForecastResponse> result =
			DailyForecastAssembler.assemble(items, LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 21));

		assertThat(result.get(0).weatherCondition()).isEqualTo(WeatherCondition.SUNNY);
	}

	@Test
	void assemble_prefersPty_overSky_whenRainOccurs() {
		List<KmaVilageFcstResponse.Item> items = List.of(
			item("SKY", "20260921", "0900", "1"), // 맑음이지만
			item("PTY", "20260921", "1500", "1")  // 비가 예보되면 RAIN이 우선해야 함
		);

		List<UpcomingWeatherResponse.DailyForecastResponse> result =
			DailyForecastAssembler.assemble(items, LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 21));

		assertThat(result.get(0).weatherCondition()).isEqualTo(WeatherCondition.RAIN);
	}

	@Test
	void assemble_mapsPtySnow_andRainSnow() {
		List<KmaVilageFcstResponse.Item> snowItems = List.of(item("PTY", "20260921", "0900", "3"));
		List<KmaVilageFcstResponse.Item> rainSnowItems = List.of(item("PTY", "20260921", "0900", "2"));

		assertThat(DailyForecastAssembler.assemble(snowItems, LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 21))
			.get(0).weatherCondition()).isEqualTo(WeatherCondition.SNOW);
		assertThat(DailyForecastAssembler.assemble(rainSnowItems, LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 21))
			.get(0).weatherCondition()).isEqualTo(WeatherCondition.RAIN_SNOW);
	}

	@Test
	void assemble_excludesDates_outsideTripRange() {
		List<KmaVilageFcstResponse.Item> items = List.of(
			item("TMP", "20260920", "0900", "15"), // 여행 시작일 전날 -> 제외
			item("TMP", "20260921", "0900", "20"), // 포함
			item("TMP", "20260925", "0900", "22")  // 여행 종료일 이후 -> 제외
		);

		List<UpcomingWeatherResponse.DailyForecastResponse> result =
			DailyForecastAssembler.assemble(items, LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 22));

		assertThat(result).extracting(UpcomingWeatherResponse.DailyForecastResponse::date)
			.containsExactly(LocalDate.of(2026, 9, 21));
	}

	@Test
	void assemble_returnsEmptyList_whenNoItemsInTripRange() {
		List<KmaVilageFcstResponse.Item> items = List.of(
			item("TMP", "20260930", "0900", "20") // 완전히 범위 밖
		);

		List<UpcomingWeatherResponse.DailyForecastResponse> result =
			DailyForecastAssembler.assemble(items, LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 22));

		assertThat(result).isEmpty();
	}

	@Test
	void assemble_returnsNullCondition_whenNoSkyOrPtyForDate() {
		List<KmaVilageFcstResponse.Item> items = List.of(
			item("TMP", "20260921", "0900", "20") // SKY/PTY가 전혀 없음 -> 임의 추정 금지, null
		);

		List<UpcomingWeatherResponse.DailyForecastResponse> result =
			DailyForecastAssembler.assemble(items, LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 21));

		assertThat(result.get(0).weatherCondition()).isNull();
	}

	private KmaVilageFcstResponse.Item item(String category, String fcstDate, String fcstTime, String value) {
		return new KmaVilageFcstResponse.Item(fcstDate, "0800", category, fcstDate, fcstTime, value, 60, 127);
	}
}
