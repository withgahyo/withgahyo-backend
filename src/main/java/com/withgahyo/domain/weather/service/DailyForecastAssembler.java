package com.withgahyo.domain.weather.service;

import com.withgahyo.domain.weather.dto.UpcomingWeatherResponse;
import com.withgahyo.domain.weather.dto.WeatherCondition;
import com.withgahyo.infra.weather.dto.KmaVilageFcstResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 기상청 단기예보 시간별 item 목록을 여행 날짜별 대표값으로 묶는 순수 계산 로직.
 *
 * <p>대표값 산정 규칙(완료 보고에도 동일하게 기재):
 * <ul>
 *   <li>weatherCondition: 그날 PTY(강수형태)가 0이 아닌 시간대가 하나라도 있으면 그중 가장 이른
 *       시각의 PTY 값을 SKY보다 우선 사용한다. 없으면 SKY 중 정오(12:00)에 가장 가까운 시각의
 *       값을 사용한다(동률이면 더 이른 시각). 둘 다 없으면 null(임의 추정하지 않음).</li>
 *   <li>minTemperature/maxTemperature: 그날 TMN/TMX가 있으면 우선 사용하고, 없으면 그날 TMP
 *       값들의 최솟값/최댓값을 사용한다.</li>
 *   <li>precipitationProbability: 그날 POP 값들의 최댓값("우산이 필요한가"를 보수적으로 판단).</li>
 * </ul>
 * 여행 기간(startDate~endDate) 밖의 날짜와, 기상청이 아예 내려주지 않은 날짜는 결과에 포함하지
 * 않는다(임의 예측 금지) — 몇 개의 날짜가 채워졌는지는 호출자가 count로 판단한다.
 */
final class DailyForecastAssembler {

	private static final DateTimeFormatter FCST_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

	private DailyForecastAssembler() {
	}

	static List<UpcomingWeatherResponse.DailyForecastResponse> assemble(
		List<KmaVilageFcstResponse.Item> items,
		LocalDate startDate,
		LocalDate endDate
	) {
		Map<LocalDate, List<KmaVilageFcstResponse.Item>> itemsByDate = items.stream()
			.filter(item -> item.fcstDate() != null)
			.collect(Collectors.groupingBy(
				item -> LocalDate.parse(item.fcstDate(), FCST_DATE_FORMAT),
				TreeMap::new,
				Collectors.toList()
			));

		List<UpcomingWeatherResponse.DailyForecastResponse> result = new ArrayList<>();
		for (Map.Entry<LocalDate, List<KmaVilageFcstResponse.Item>> entry : itemsByDate.entrySet()) {
			LocalDate date = entry.getKey();
			if (date.isBefore(startDate) || date.isAfter(endDate)) {
				continue;
			}
			List<KmaVilageFcstResponse.Item> dayItems = entry.getValue();
			result.add(new UpcomingWeatherResponse.DailyForecastResponse(
				date,
				resolveCondition(dayItems),
				resolveMinTemperature(dayItems),
				resolveMaxTemperature(dayItems),
				resolvePrecipitationProbability(dayItems)
			));
		}
		return result;
	}

	private static WeatherCondition resolveCondition(List<KmaVilageFcstResponse.Item> dayItems) {
		List<KmaVilageFcstResponse.Item> rainingHours = dayItems.stream()
			.filter(item -> "PTY".equals(item.category()))
			.filter(item -> {
				Integer value = parseIntOrNull(item.fcstValue());
				return value != null && value != 0;
			})
			.sorted(Comparator.comparing(
				item -> Objects.requireNonNullElse(item.fcstTime(), "9999")
			))
			.toList();
		if (!rainingHours.isEmpty()) {
			return mapPty(parseIntOrNull(rainingHours.get(0).fcstValue()));
		}

		return dayItems.stream()
			.filter(item -> "SKY".equals(item.category()))
			.filter(item -> item.fcstTime() != null)
			.min(Comparator.comparingInt(item -> Math.abs(hourOf(item.fcstTime()) - 12)))
			.map(item -> mapSky(parseIntOrNull(item.fcstValue())))
			.orElse(null);
	}

	private static Integer resolveMinTemperature(List<KmaVilageFcstResponse.Item> dayItems) {
		return numericValues(dayItems, "TMN").findFirst()
			.orElseGet(() -> numericValues(dayItems, "TMP").min(Integer::compareTo).orElse(null));
	}

	private static Integer resolveMaxTemperature(List<KmaVilageFcstResponse.Item> dayItems) {
		return numericValues(dayItems, "TMX").findFirst()
			.orElseGet(() -> numericValues(dayItems, "TMP").max(Integer::compareTo).orElse(null));
	}

	private static Integer resolvePrecipitationProbability(List<KmaVilageFcstResponse.Item> dayItems) {
		return numericValues(dayItems, "POP").max(Integer::compareTo).orElse(null);
	}

	private static Stream<Integer> numericValues(List<KmaVilageFcstResponse.Item> dayItems, String category) {
		return dayItems.stream()
			.filter(item -> category.equals(item.category()))
			.map(item -> parseIntOrNull(item.fcstValue()))
			.filter(Objects::nonNull);
	}

	private static int hourOf(String fcstTime) {
		if (fcstTime == null || fcstTime.length() < 2) {
			return Integer.MAX_VALUE;
		}
		try {
			return Integer.parseInt(fcstTime.substring(0, 2));
		} catch (NumberFormatException exception) {
			return Integer.MAX_VALUE;
		}
	}

	private static WeatherCondition mapPty(Integer pty) {
		if (pty == null) {
			return null;
		}
		return switch (pty) {
			case 1, 4 -> WeatherCondition.RAIN;
			case 2 -> WeatherCondition.RAIN_SNOW;
			case 3 -> WeatherCondition.SNOW;
			default -> null;
		};
	}

	private static WeatherCondition mapSky(Integer sky) {
		if (sky == null) {
			return null;
		}
		return switch (sky) {
			case 1 -> WeatherCondition.SUNNY;
			case 3, 4 -> WeatherCondition.CLOUDY;
			default -> null;
		};
	}

	private static Integer parseIntOrNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return (int) Math.round(Double.parseDouble(value.trim()));
		} catch (NumberFormatException exception) {
			return null;
		}
	}
}
