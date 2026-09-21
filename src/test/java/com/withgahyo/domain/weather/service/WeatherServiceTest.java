package com.withgahyo.domain.weather.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.withgahyo.domain.course.entity.Course;
import com.withgahyo.domain.course.entity.CourseScheduleItem;
import com.withgahyo.domain.course.repository.CourseRepository;
import com.withgahyo.domain.course.repository.CourseScheduleItemRepository;
import com.withgahyo.domain.place.entity.Place;
import com.withgahyo.domain.place.entity.Region;
import com.withgahyo.domain.user.entity.User;
import com.withgahyo.domain.weather.dto.UpcomingWeatherResponse;
import com.withgahyo.domain.weather.dto.WeatherStatus;
import com.withgahyo.infra.weather.WeatherApiException;
import com.withgahyo.infra.weather.WeatherClient;
import com.withgahyo.infra.weather.dto.KmaVilageFcstResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

	@Mock
	private CourseRepository courseRepository;

	@Mock
	private CourseScheduleItemRepository courseScheduleItemRepository;

	@Mock
	private WeatherClient weatherClient;

	private WeatherService weatherService;

	@BeforeEach
	void setUp() {
		weatherService = new WeatherService(courseRepository, courseScheduleItemRepository, weatherClient);
	}

	@Test
	void getUpcomingWeather_returnsNoUpcomingTrip_whenNoCourse() {
		given(courseRepository.findUpcomingCoursesFromDateForUser(ArgumentMatchers.eq(1L), ArgumentMatchers.any()))
			.willReturn(List.of());

		UpcomingWeatherResponse response = weatherService.getUpcomingWeather(1L);

		assertThat(response.status()).isEqualTo(WeatherStatus.NO_UPCOMING_TRIP);
		assertThat(response.courseId()).isNull();
		assertThat(response.dailyForecasts()).isEmpty();
	}

	@Test
	void getUpcomingWeather_returnsLocationUnavailable_whenNoScheduleItem() {
		Course course = courseWithId(301L, "대전 가족 여행", LocalDate.of(2026, 9, 25));
		given(courseRepository.findUpcomingCoursesFromDateForUser(ArgumentMatchers.eq(1L), ArgumentMatchers.any()))
			.willReturn(List.of(course));
		given(courseScheduleItemRepository.findAllByCourseId(301L)).willReturn(List.of());

		UpcomingWeatherResponse response = weatherService.getUpcomingWeather(1L);

		assertThat(response.status()).isEqualTo(WeatherStatus.LOCATION_UNAVAILABLE);
		assertThat(response.courseId()).isEqualTo(301L);
		verify(weatherClient, never()).getShortTermForecast(anyInt(), anyInt());
	}

	@Test
	void getUpcomingWeather_returnsLocationUnavailable_whenPlaceHasNoCoordinates() {
		Course course = courseWithId(301L, "대전 가족 여행", LocalDate.of(2026, 9, 25));
		Place placeWithoutCoords = placeWithId(501L, null, null);
		given(courseRepository.findUpcomingCoursesFromDateForUser(ArgumentMatchers.eq(1L), ArgumentMatchers.any()))
			.willReturn(List.of(course));
		given(courseScheduleItemRepository.findAllByCourseId(301L))
			.willReturn(List.of(scheduleItem(course, placeWithoutCoords, 1, 1)));

		UpcomingWeatherResponse response = weatherService.getUpcomingWeather(1L);

		assertThat(response.status()).isEqualTo(WeatherStatus.LOCATION_UNAVAILABLE);
		verify(weatherClient, never()).getShortTermForecast(anyInt(), anyInt());
	}

	@Test
	void getUpcomingWeather_usesDay1FirstVisitOrderPlace_forGridConversion() {
		Course course = courseWithId(301L, "대전 가족 여행", LocalDate.of(2026, 9, 25));
		Place day1Second = placeWithId(502L, new BigDecimal("37.0"), new BigDecimal("127.0"));
		Place day1First = placeWithId(501L, new BigDecimal("37.5665"), new BigDecimal("126.9780")); // 서울시청
		Place day2First = placeWithId(503L, new BigDecimal("35.0"), new BigDecimal("129.0"));
		given(courseRepository.findUpcomingCoursesFromDateForUser(ArgumentMatchers.eq(1L), ArgumentMatchers.any()))
			.willReturn(List.of(course));
		// 정렬 순서와 무관하게 "Day1 + visitOrder 최소"를 골라야 하므로 일부러 뒤섞어 리턴한다.
		given(courseScheduleItemRepository.findAllByCourseId(301L)).willReturn(List.of(
			scheduleItem(course, day1First, 1, 1),
			scheduleItem(course, day1Second, 1, 2),
			scheduleItem(course, day2First, 2, 1)
		));
		given(weatherClient.getShortTermForecast(60, 127)).willReturn(List.of());

		weatherService.getUpcomingWeather(1L);

		verify(weatherClient).getShortTermForecast(60, 127);
	}

	@Test
	void getUpcomingWeather_returnsExternalApiError_whenClientThrows() {
		Course course = courseWithId(301L, "대전 가족 여행", LocalDate.of(2026, 9, 25));
		Place place = placeWithId(501L, new BigDecimal("37.5665"), new BigDecimal("126.9780"));
		given(courseRepository.findUpcomingCoursesFromDateForUser(ArgumentMatchers.eq(1L), ArgumentMatchers.any()))
			.willReturn(List.of(course));
		given(courseScheduleItemRepository.findAllByCourseId(301L))
			.willReturn(List.of(scheduleItem(course, place, 1, 1)));
		given(weatherClient.getShortTermForecast(anyInt(), anyInt()))
			.willThrow(new WeatherApiException("기상청 오류"));

		UpcomingWeatherResponse response = weatherService.getUpcomingWeather(1L);

		assertThat(response.status()).isEqualTo(WeatherStatus.EXTERNAL_API_ERROR);
		assertThat(response.courseId()).isEqualTo(301L);
		assertThat(response.dailyForecasts()).isEmpty();
	}

	@Test
	void getUpcomingWeather_returnsAvailable_whenAllTripDatesForecast() {
		Course course = courseWithId(301L, "대전 가족 여행", LocalDate.of(2026, 9, 25)); // startDate ~ +1일
		Place place = placeWithId(501L, new BigDecimal("37.5665"), new BigDecimal("126.9780"));
		given(courseRepository.findUpcomingCoursesFromDateForUser(ArgumentMatchers.eq(1L), ArgumentMatchers.any()))
			.willReturn(List.of(course));
		given(courseScheduleItemRepository.findAllByCourseId(301L))
			.willReturn(List.of(scheduleItem(course, place, 1, 1)));
		given(weatherClient.getShortTermForecast(60, 127)).willReturn(List.of(
			new KmaVilageFcstResponse.Item("20260921", "0800", "TMP", "20260925", "0900", "20", 60, 127),
			new KmaVilageFcstResponse.Item("20260921", "0800", "SKY", "20260925", "0900", "1", 60, 127),
			new KmaVilageFcstResponse.Item("20260921", "0800", "TMP", "20260926", "0900", "18", 60, 127),
			new KmaVilageFcstResponse.Item("20260921", "0800", "SKY", "20260926", "0900", "1", 60, 127)
		));

		UpcomingWeatherResponse response = weatherService.getUpcomingWeather(1L);

		assertThat(response.status()).isEqualTo(WeatherStatus.AVAILABLE);
		assertThat(response.dailyForecasts()).hasSize(2);
	}

	@Test
	void getUpcomingWeather_returnsPartiallyAvailable_whenOnlySomeTripDatesForecast() {
		// 2박 3일(3일치 예보 필요)인데 응답에는 2일치만 들어있는 상황
		Course threeDayCourse = new TestCourseFactory().courseWithDates(
			301L, "대전 가족 여행", LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 27)
		);
		Place place = placeWithId(501L, new BigDecimal("37.5665"), new BigDecimal("126.9780"));
		given(courseRepository.findUpcomingCoursesFromDateForUser(ArgumentMatchers.eq(1L), ArgumentMatchers.any()))
			.willReturn(List.of(threeDayCourse));
		given(courseScheduleItemRepository.findAllByCourseId(301L))
			.willReturn(List.of(scheduleItem(threeDayCourse, place, 1, 1)));
		given(weatherClient.getShortTermForecast(60, 127)).willReturn(List.of(
			// 27일치는 단기예보 범위 밖이라 응답에 아예 없음
			new KmaVilageFcstResponse.Item("20260921", "0800", "TMP", "20260925", "0900", "20", 60, 127),
			new KmaVilageFcstResponse.Item("20260921", "0800", "TMP", "20260926", "0900", "18", 60, 127)
		));

		UpcomingWeatherResponse response = weatherService.getUpcomingWeather(1L);

		assertThat(response.status()).isEqualTo(WeatherStatus.PARTIALLY_AVAILABLE);
		assertThat(response.dailyForecasts()).hasSize(2);
	}

	@Test
	void getUpcomingWeather_returnsOutOfForecastRange_whenNoTripDateForecast() {
		Course course = courseWithId(301L, "대전 가족 여행", LocalDate.of(2026, 9, 25));
		Place place = placeWithId(501L, new BigDecimal("37.5665"), new BigDecimal("126.9780"));
		given(courseRepository.findUpcomingCoursesFromDateForUser(ArgumentMatchers.eq(1L), ArgumentMatchers.any()))
			.willReturn(List.of(course));
		given(courseScheduleItemRepository.findAllByCourseId(301L))
			.willReturn(List.of(scheduleItem(course, place, 1, 1)));
		given(weatherClient.getShortTermForecast(60, 127)).willReturn(List.of(
			new KmaVilageFcstResponse.Item("20260921", "0800", "TMP", "20260922", "0900", "20", 60, 127) // 여행 시작 전 날짜뿐
		));

		UpcomingWeatherResponse response = weatherService.getUpcomingWeather(1L);

		assertThat(response.status()).isEqualTo(WeatherStatus.OUT_OF_FORECAST_RANGE);
		assertThat(response.dailyForecasts()).isEmpty();
	}

	private Course courseWithId(Long courseId, String title, LocalDate startDate) {
		return new TestCourseFactory().courseWithDates(courseId, title, startDate, startDate.plusDays(1));
	}

	private Place placeWithId(Long placeId, BigDecimal latitude, BigDecimal longitude) {
		Place place = Place.create(
			"CONTENT-" + placeId, "12", "KAKAO", "TOURIST_ATTRACTION",
			Region.create("3", "1", "대전광역시 동구"), "장소" + placeId, "대전광역시 서구 어딘가",
			latitude, longitude, null
		);
		ReflectionTestUtils.setField(place, "placeId", placeId);
		return place;
	}

	private CourseScheduleItem scheduleItem(Course course, Place place, int dayNumber, int visitOrder) {
		return CourseScheduleItem.create(course, place, dayNumber, visitOrder, null, null, null, null, null);
	}

	/** courseWithId 시그니처를 재사용하면서 endDate를 임의로 조정할 수 있게 하기 위한 작은 헬퍼. */
	private static final class TestCourseFactory {
		Course courseWithDates(Long courseId, String title, LocalDate startDate, LocalDate endDate) {
			User creator = User.create("KAKAO", "creator", "작성자", null);
			Region region = Region.create("3", "1", "대전광역시 동구");
			Course course = Course.create(creator, region, title, startDate, endDate);
			ReflectionTestUtils.setField(course, "courseId", courseId);
			return course;
		}
	}
}
