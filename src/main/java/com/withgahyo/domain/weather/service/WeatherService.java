package com.withgahyo.domain.weather.service;

import com.withgahyo.domain.course.entity.Course;
import com.withgahyo.domain.course.entity.CourseScheduleItem;
import com.withgahyo.domain.course.repository.CourseRepository;
import com.withgahyo.domain.course.repository.CourseScheduleItemRepository;
import com.withgahyo.domain.place.entity.Place;
import com.withgahyo.domain.weather.dto.UpcomingWeatherResponse;
import com.withgahyo.infra.weather.WeatherApiException;
import com.withgahyo.infra.weather.WeatherClient;
import com.withgahyo.infra.weather.WeatherGridConverter;
import com.withgahyo.infra.weather.dto.KmaVilageFcstResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WeatherService {

	private final CourseRepository courseRepository;
	private final CourseScheduleItemRepository courseScheduleItemRepository;
	private final WeatherClient weatherClient;

	public WeatherService(
		CourseRepository courseRepository,
		CourseScheduleItemRepository courseScheduleItemRepository,
		WeatherClient weatherClient
	) {
		this.courseRepository = courseRepository;
		this.courseScheduleItemRepository = courseScheduleItemRepository;
		this.weatherClient = weatherClient;
	}

	@Transactional(readOnly = true)
	public UpcomingWeatherResponse getUpcomingWeather(Long userId) {
		Course course = findClosestUpcomingCourse(userId);
		if (course == null) {
			return UpcomingWeatherResponse.noUpcomingTrip();
		}

		Place place = findRepresentativePlace(course.getCourseId());
		if (place == null || place.getLatitude() == null || place.getLongitude() == null) {
			return UpcomingWeatherResponse.locationUnavailable(course);
		}

		WeatherGridConverter.Grid grid = WeatherGridConverter.toGrid(place.getLatitude(), place.getLongitude());

		List<KmaVilageFcstResponse.Item> items;
		try {
			items = weatherClient.getShortTermForecast(grid.nx(), grid.ny());
		} catch (WeatherApiException exception) {
			// 날씨 API 장애가 Course/Home 등 다른 기능에 영향을 주면 안 되므로 여기서 흡수하고
			// 상태만 EXTERNAL_API_ERROR로 내려준다(예외를 컨트롤러까지 전파하지 않음).
			return UpcomingWeatherResponse.externalApiError(course);
		}

		List<UpcomingWeatherResponse.DailyForecastResponse> dailyForecasts =
			DailyForecastAssembler.assemble(items, course.getStartDate(), course.getEndDate());
		return UpcomingWeatherResponse.of(course, dailyForecasts);
	}

	private Course findClosestUpcomingCourse(Long userId) {
		List<Course> courses = courseRepository.findUpcomingCoursesFromDateForUser(userId, LocalDate.now());
		return courses.isEmpty() ? null : courses.get(0);
	}

	// Day 1 -> visitOrder가 가장 빠른 CourseScheduleItem의 Place. schedule이 없거나 Day 1
	// 항목이 없으면 null을 반환해 상위에서 LOCATION_UNAVAILABLE로 처리하게 한다.
	private Place findRepresentativePlace(Long courseId) {
		return courseScheduleItemRepository.findAllByCourseId(courseId).stream()
			.filter(item -> item.getDayNumber() != null && item.getDayNumber() == 1)
			.findFirst()
			.map(CourseScheduleItem::getPlace)
			.orElse(null);
	}
}
