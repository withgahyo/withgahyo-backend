package com.withgahyo.domain.weather.dto;

import com.withgahyo.domain.course.entity.Course;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public record UpcomingWeatherResponse(
	WeatherStatus status,
	Long courseId,
	String courseTitle,
	String regionName,
	LocalDate startDate,
	LocalDate endDate,
	Long daysUntilTrip,
	List<DailyForecastResponse> dailyForecasts
) {

	public static UpcomingWeatherResponse noUpcomingTrip() {
		return new UpcomingWeatherResponse(WeatherStatus.NO_UPCOMING_TRIP, null, null, null, null, null, null, List.of());
	}

	public static UpcomingWeatherResponse locationUnavailable(Course course) {
		return courseOnly(course, WeatherStatus.LOCATION_UNAVAILABLE);
	}

	public static UpcomingWeatherResponse externalApiError(Course course) {
		return courseOnly(course, WeatherStatus.EXTERNAL_API_ERROR);
	}

	public static UpcomingWeatherResponse of(Course course, List<DailyForecastResponse> dailyForecasts) {
		long tripLengthDays = ChronoUnit.DAYS.between(course.getStartDate(), course.getEndDate()) + 1;
		WeatherStatus status;
		if (dailyForecasts.isEmpty()) {
			status = WeatherStatus.OUT_OF_FORECAST_RANGE;
		} else if (dailyForecasts.size() < tripLengthDays) {
			status = WeatherStatus.PARTIALLY_AVAILABLE;
		} else {
			status = WeatherStatus.AVAILABLE;
		}
		return new UpcomingWeatherResponse(
			status,
			course.getCourseId(),
			course.getTitle(),
			course.getRegion().getName(),
			course.getStartDate(),
			course.getEndDate(),
			course.daysUntilTrip(),
			dailyForecasts
		);
	}

	private static UpcomingWeatherResponse courseOnly(Course course, WeatherStatus status) {
		return new UpcomingWeatherResponse(
			status,
			course.getCourseId(),
			course.getTitle(),
			course.getRegion().getName(),
			course.getStartDate(),
			course.getEndDate(),
			course.daysUntilTrip(),
			List.of()
		);
	}

	public record DailyForecastResponse(
		LocalDate date,
		WeatherCondition weatherCondition,
		Integer minTemperature,
		Integer maxTemperature,
		Integer precipitationProbability
	) {
	}
}
