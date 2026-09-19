package com.withgahyo.domain.home.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.withgahyo.domain.course.entity.Course;
import com.withgahyo.domain.course.entity.CourseInterestKeyword;
import com.withgahyo.domain.course.entity.CourseKeyword;
import com.withgahyo.domain.course.entity.CourseScheduleItem;
import com.withgahyo.domain.course.repository.CourseKeywordRepository;
import com.withgahyo.domain.course.repository.CourseRepository;
import com.withgahyo.domain.course.repository.CourseScheduleItemRepository;
import com.withgahyo.domain.home.dto.HomeResponse;
import com.withgahyo.domain.place.entity.Place;
import com.withgahyo.domain.place.entity.Region;
import com.withgahyo.domain.recommendation.entity.RecommendationCandidate;
import com.withgahyo.domain.recommendation.entity.RecommendationCandidateItem;
import com.withgahyo.domain.recommendation.entity.RecommendationJob;
import com.withgahyo.domain.recommendation.repository.RecommendationCandidateItemRepository;
import com.withgahyo.domain.recommendation.repository.RecommendationCandidateRepository;
import com.withgahyo.domain.user.entity.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

	@Mock
	private CourseRepository courseRepository;

	@Mock
	private CourseKeywordRepository courseKeywordRepository;

	@Mock
	private CourseScheduleItemRepository courseScheduleItemRepository;

	@Mock
	private RecommendationCandidateRepository recommendationCandidateRepository;

	@Mock
	private RecommendationCandidateItemRepository recommendationCandidateItemRepository;

	private HomeService homeService;

	@BeforeEach
	void setUp() {
		homeService = new HomeService(
			courseRepository,
			courseKeywordRepository,
			courseScheduleItemRepository,
			recommendationCandidateRepository,
			recommendationCandidateItemRepository
		);
	}

	@Test
	void getHome_returnsFamilyCourses_whenCoursesExist() {
		Course course = courseWithId(301L, "대전 가족 여행", LocalDate.now().plusDays(6));
		stubBaseCourse(course);
		stubNoAlternatives(course);

		HomeResponse response = homeService.getHome(1L);

		assertThat(response.familyCourses()).hasSize(1);
		HomeResponse.FamilyCourseResponse familyCourse = response.familyCourses().get(0);
		assertThat(familyCourse.courseId()).isEqualTo(301L);
		assertThat(familyCourse.title()).isEqualTo("대전 가족 여행");
		assertThat(familyCourse.regionName()).isEqualTo("대전광역시 동구");
	}

	@Test
	void getHome_returnsEmptyFamilyCourses_whenNoCourse() {
		given(courseRepository.findUpcomingCoursesForUser(1L)).willReturn(List.of());

		HomeResponse response = homeService.getHome(1L);

		assertThat(response.familyCourses()).isEmpty();
		// courseIds가 비어 있으면 IN () 쿼리가 나가지 않도록 태그/일정/후보 조회 자체를 생략해야 한다.
		verify(courseKeywordRepository, never()).findAllByCourseIdIn(anyList());
		verify(courseScheduleItemRepository, never()).findAllByCourseIdIn(anyList());
		verify(recommendationCandidateRepository, never()).findAllByJobCourseIdIn(anyList());
	}

	@Test
	void getHome_mapsTags_fromCourseKeyword() {
		Course course = courseWithId(301L, "대전 가족 여행", LocalDate.now().plusDays(6));
		stubBaseCourse(course);
		given(courseKeywordRepository.findAllByCourseIdIn(List.of(301L))).willReturn(List.of(
			courseKeyword(course, "BARRIER_FREE", "무장애"),
			courseKeyword(course, "FOOD_TOUR", "맛집")
		));
		given(courseScheduleItemRepository.findAllByCourseIdIn(List.of(301L))).willReturn(List.of());
		given(recommendationCandidateRepository.findAllByJobCourseIdIn(List.of(301L))).willReturn(List.of());

		HomeResponse response = homeService.getHome(1L);

		assertThat(response.familyCourses().get(0).tags()).containsExactly("무장애", "맛집");
	}

	@Test
	void getHome_returnsEmptyTagList_whenCourseHasNoKeyword() {
		Course course = courseWithId(301L, "대전 가족 여행", LocalDate.now().plusDays(6));
		stubBaseCourse(course);
		stubNoAlternatives(course);

		HomeResponse response = homeService.getHome(1L);

		assertThat(response.familyCourses().get(0).tags()).isEmpty();
	}

	@Test
	void getHome_usesFirstPlaceImageUrl_asThumbnail() {
		Course course = courseWithId(301L, "대전 가족 여행", LocalDate.now().plusDays(6));
		stubBaseCourse(course);
		given(courseKeywordRepository.findAllByCourseIdIn(List.of(301L))).willReturn(List.of());
		given(courseScheduleItemRepository.findAllByCourseIdIn(List.of(301L))).willReturn(List.of(
			scheduleItem(course, placeWithImageUrl(501L, "https://example.com/first.jpg"), 1, 1),
			scheduleItem(course, placeWithImageUrl(502L, "https://example.com/second.jpg"), 1, 2)
		));
		given(recommendationCandidateRepository.findAllByJobCourseIdIn(List.of(301L))).willReturn(List.of());

		HomeResponse response = homeService.getHome(1L);

		assertThat(response.familyCourses().get(0).imageUrl()).isEqualTo("https://example.com/first.jpg");
	}

	@Test
	void getHome_skipsNullOrBlankImageUrl_andUsesNextPlaceImage() {
		Course course = courseWithId(301L, "대전 가족 여행", LocalDate.now().plusDays(6));
		stubBaseCourse(course);
		given(courseKeywordRepository.findAllByCourseIdIn(List.of(301L))).willReturn(List.of());
		given(courseScheduleItemRepository.findAllByCourseIdIn(List.of(301L))).willReturn(List.of(
			scheduleItem(course, placeWithImageUrl(501L, null), 1, 1),
			scheduleItem(course, placeWithImageUrl(502L, "   "), 1, 2),
			scheduleItem(course, placeWithImageUrl(503L, "https://example.com/third.jpg"), 1, 3)
		));
		given(recommendationCandidateRepository.findAllByJobCourseIdIn(List.of(301L))).willReturn(List.of());

		HomeResponse response = homeService.getHome(1L);

		assertThat(response.familyCourses().get(0).imageUrl()).isEqualTo("https://example.com/third.jpg");
	}

	@Test
	void getHome_returnsNullImageUrl_whenNoPlaceHasImage() {
		Course course = courseWithId(301L, "대전 가족 여행", LocalDate.now().plusDays(6));
		stubBaseCourse(course);
		given(courseKeywordRepository.findAllByCourseIdIn(List.of(301L))).willReturn(List.of());
		given(courseScheduleItemRepository.findAllByCourseIdIn(List.of(301L))).willReturn(List.of(
			scheduleItem(course, placeWithImageUrl(501L, null), 1, 1)
		));
		given(recommendationCandidateRepository.findAllByJobCourseIdIn(List.of(301L))).willReturn(List.of());

		HomeResponse response = homeService.getHome(1L);

		assertThat(response.familyCourses().get(0).imageUrl()).isNull();
	}

	@Test
	void getHome_doesNotMixTagsOrCourseImages_acrossMultipleCourses() {
		Course first = courseWithId(301L, "대전 가족 여행", LocalDate.now().plusDays(3));
		Course second = courseWithId(302L, "부산 가족 여행", LocalDate.now().plusDays(10));
		given(courseRepository.findUpcomingCoursesForUser(1L)).willReturn(List.of(first, second));
		given(courseKeywordRepository.findAllByCourseIdIn(List.of(301L, 302L))).willReturn(List.of(
			courseKeyword(first, "BARRIER_FREE", "무장애"),
			courseKeyword(second, "FOOD_TOUR", "맛집")
		));
		given(courseScheduleItemRepository.findAllByCourseIdIn(List.of(301L, 302L))).willReturn(List.of(
			scheduleItem(first, placeWithImageUrl(501L, "https://example.com/first.jpg"), 1, 1),
			scheduleItem(second, placeWithImageUrl(502L, null), 1, 1)
		));
		given(recommendationCandidateRepository.findAllByJobCourseIdIn(List.of(301L, 302L))).willReturn(List.of());

		HomeResponse response = homeService.getHome(1L);

		HomeResponse.FamilyCourseResponse firstResponse = response.familyCourses().stream()
			.filter(course -> course.courseId().equals(301L)).findFirst().orElseThrow();
		HomeResponse.FamilyCourseResponse secondResponse = response.familyCourses().stream()
			.filter(course -> course.courseId().equals(302L)).findFirst().orElseThrow();

		assertThat(firstResponse.tags()).containsExactly("무장애");
		assertThat(firstResponse.imageUrl()).isEqualTo("https://example.com/first.jpg");
		assertThat(secondResponse.tags()).containsExactly("맛집");
		assertThat(secondResponse.imageUrl()).isNull();
	}

	@Test
	void getHome_computesDaysUntilTrip_sameAsCourseEntityMethod() {
		Course course = courseWithId(301L, "대전 가족 여행", LocalDate.now().plusDays(6));
		stubBaseCourse(course);
		stubNoAlternatives(course);

		HomeResponse response = homeService.getHome(1L);

		// CourseDetailResponse와 동일하게 Course.daysUntilTrip()을 그대로 사용하는지 확인한다.
		assertThat(response.familyCourses().get(0).daysUntilTrip()).isEqualTo(course.daysUntilTrip());
	}

	@Test
	void getHome_returnsNegativeDaysUntilTrip_whenStartDateIsInThePast() {
		// 회귀 방지: startDate가 지난 UPCOMING 코스도 Home에는 노출되며(status만으로 필터링),
		// daysUntilTrip은 0으로 보정되지 않고 signed 값(음수)이어야 한다.
		Course course = courseWithId(301L, "지난 여행", LocalDate.now().minusDays(13));
		stubBaseCourse(course);
		stubNoAlternatives(course);

		HomeResponse response = homeService.getHome(1L);

		assertThat(response.familyCourses().get(0).daysUntilTrip()).isEqualTo(-13L);
	}

	// ---- alternativeCandidates ----

	@Test
	void getHome_returnsAlternativeCandidates_excludingSelected() {
		// Case 1, 2: Job 1개, 후보 A/B/C 중 B가 선택 -> alternativeCandidates = [A, C]
		Course course = courseWithId(301L, "대전 가족 여행", LocalDate.now().plusDays(6));
		RecommendationJob job = jobWithId(701L, course);
		RecommendationCandidate a = candidateWithId(1001L, job, 1, false, "여유로운 대전 산책", "요약A");
		RecommendationCandidate b = candidateWithId(1002L, job, 2, true, "대전 가족 여행", "요약B");
		RecommendationCandidate c = candidateWithId(1003L, job, 3, false, "자연과 함께하는 대전 여행", "요약C");

		stubBaseCourse(course);
		stubNoTagsOrSchedule(course);
		given(recommendationCandidateRepository.findAllByJobCourseIdIn(List.of(301L)))
			.willReturn(List.of(a, b, c));
		given(recommendationCandidateItemRepository.findAllWithPlaceByRecommendationCandidateIdIn(List.of(1001L, 1003L)))
			.willReturn(List.of());

		HomeResponse response = homeService.getHome(1L);

		List<HomeResponse.AlternativeCandidateResponse> alternatives =
			response.familyCourses().get(0).alternativeCandidates();
		assertThat(alternatives).extracting(HomeResponse.AlternativeCandidateResponse::candidateId)
			.containsExactly(1001L, 1003L);
		assertThat(alternatives).noneMatch(candidate -> candidate.candidateId().equals(1002L));
	}

	@Test
	void getHome_keepsAlternativeCandidatesInRankOrder() {
		// Case 3: rank 1=선택, 2,3=미선택이면 2,3 순서 그대로 유지(재정렬 없음)
		Course course = courseWithId(301L, "대전 가족 여행", LocalDate.now().plusDays(6));
		RecommendationJob job = jobWithId(701L, course);
		RecommendationCandidate rank1Selected = candidateWithId(1001L, job, 1, true, "선택된 코스", "요약");
		RecommendationCandidate rank2 = candidateWithId(1002L, job, 2, false, "2순위 코스", "요약2");
		RecommendationCandidate rank3 = candidateWithId(1003L, job, 3, false, "3순위 코스", "요약3");

		stubBaseCourse(course);
		stubNoTagsOrSchedule(course);
		// 실제 쿼리는 rank asc 로 정렬해서 돌려주므로 목 리턴도 그 순서(1,2,3)를 그대로 재현한다.
		given(recommendationCandidateRepository.findAllByJobCourseIdIn(List.of(301L)))
			.willReturn(List.of(rank1Selected, rank2, rank3));
		given(recommendationCandidateItemRepository.findAllWithPlaceByRecommendationCandidateIdIn(List.of(1002L, 1003L)))
			.willReturn(List.of());

		HomeResponse response = homeService.getHome(1L);

		assertThat(response.familyCourses().get(0).alternativeCandidates())
			.extracting(HomeResponse.AlternativeCandidateResponse::candidateId)
			.containsExactly(1002L, 1003L);
	}

	@Test
	void getHome_mapsAlternativeCandidateTitleAndSummary() {
		// Case 4
		Course course = courseWithId(301L, "대전 가족 여행", LocalDate.now().plusDays(6));
		RecommendationJob job = jobWithId(701L, course);
		RecommendationCandidate selected = candidateWithId(1001L, job, 1, true, "선택된 코스", "선택 요약");
		RecommendationCandidate alternative = candidateWithId(1002L, job, 2, false, "여유로운 대전 산책", "산책 위주 코스입니다.");

		stubBaseCourse(course);
		stubNoTagsOrSchedule(course);
		given(recommendationCandidateRepository.findAllByJobCourseIdIn(List.of(301L)))
			.willReturn(List.of(selected, alternative));
		given(recommendationCandidateItemRepository.findAllWithPlaceByRecommendationCandidateIdIn(List.of(1002L)))
			.willReturn(List.of());

		HomeResponse response = homeService.getHome(1L);

		HomeResponse.AlternativeCandidateResponse response1 =
			response.familyCourses().get(0).alternativeCandidates().get(0);
		assertThat(response1.title()).isEqualTo("여유로운 대전 산책");
		assertThat(response1.summary()).isEqualTo("산책 위주 코스입니다.");
	}

	@Test
	void getHome_usesFirstNonNullPlaceImage_asAlternativeCandidateThumbnail() {
		// Case 5
		Course course = courseWithId(301L, "대전 가족 여행", LocalDate.now().plusDays(6));
		RecommendationJob job = jobWithId(701L, course);
		RecommendationCandidate selected = candidateWithId(1001L, job, 1, true, "선택된 코스", "요약");
		RecommendationCandidate alternative = candidateWithId(1002L, job, 2, false, "대안 코스", "요약");

		stubBaseCourse(course);
		stubNoTagsOrSchedule(course);
		given(recommendationCandidateRepository.findAllByJobCourseIdIn(List.of(301L)))
			.willReturn(List.of(selected, alternative));
		given(recommendationCandidateItemRepository.findAllWithPlaceByRecommendationCandidateIdIn(List.of(1002L)))
			.willReturn(List.of(
				candidateItem(2001L, alternative, placeWithImageUrl(501L, null), 1, 1),
				candidateItem(2002L, alternative, placeWithImageUrl(502L, "https://example.com/alt.jpg"), 1, 2)
			));

		HomeResponse response = homeService.getHome(1L);

		assertThat(response.familyCourses().get(0).alternativeCandidates().get(0).thumbnailImageUrl())
			.isEqualTo("https://example.com/alt.jpg");
	}

	@Test
	void getHome_returnsNullThumbnail_whenAlternativeCandidateHasNoImage() {
		// Case 6
		Course course = courseWithId(301L, "대전 가족 여행", LocalDate.now().plusDays(6));
		RecommendationJob job = jobWithId(701L, course);
		RecommendationCandidate selected = candidateWithId(1001L, job, 1, true, "선택된 코스", "요약");
		RecommendationCandidate alternative = candidateWithId(1002L, job, 2, false, "대안 코스", "요약");

		stubBaseCourse(course);
		stubNoTagsOrSchedule(course);
		given(recommendationCandidateRepository.findAllByJobCourseIdIn(List.of(301L)))
			.willReturn(List.of(selected, alternative));
		given(recommendationCandidateItemRepository.findAllWithPlaceByRecommendationCandidateIdIn(List.of(1002L)))
			.willReturn(List.of(candidateItem(2001L, alternative, placeWithImageUrl(501L, "   "), 1, 1)));

		HomeResponse response = homeService.getHome(1L);

		assertThat(response.familyCourses().get(0).alternativeCandidates().get(0).thumbnailImageUrl()).isNull();
	}

	@Test
	void getHome_returnsAlternativesFromConfirmedJobOnly_whenCourseHasMultipleGenerations() {
		// Case 7: Job1(A/B/C 전부 미선택, 버려진 재시도) / Job2(D 미선택, E 선택, F 미선택)
		// -> alternativeCandidates = [D, F], A/B/C는 절대 포함되지 않아야 한다.
		Course course = courseWithId(301L, "대전 가족 여행", LocalDate.now().plusDays(6));
		RecommendationJob abandonedJob = jobWithId(701L, course);
		RecommendationCandidate a = candidateWithId(1001L, abandonedJob, 1, false, "A", "요약");
		RecommendationCandidate b = candidateWithId(1002L, abandonedJob, 2, false, "B", "요약");
		RecommendationCandidate c = candidateWithId(1003L, abandonedJob, 3, false, "C", "요약");

		RecommendationJob confirmedJob = jobWithId(702L, course);
		RecommendationCandidate d = candidateWithId(1004L, confirmedJob, 1, false, "D", "요약");
		RecommendationCandidate e = candidateWithId(1005L, confirmedJob, 2, true, "E", "요약");
		RecommendationCandidate f = candidateWithId(1006L, confirmedJob, 3, false, "F", "요약");

		stubBaseCourse(course);
		stubNoTagsOrSchedule(course);
		given(recommendationCandidateRepository.findAllByJobCourseIdIn(List.of(301L)))
			.willReturn(List.of(a, b, c, d, e, f));
		given(recommendationCandidateItemRepository.findAllWithPlaceByRecommendationCandidateIdIn(List.of(1004L, 1006L)))
			.willReturn(List.of());

		HomeResponse response = homeService.getHome(1L);

		assertThat(response.familyCourses().get(0).alternativeCandidates())
			.extracting(HomeResponse.AlternativeCandidateResponse::candidateId)
			.containsExactly(1004L, 1006L);
	}

	@Test
	void getHome_returnsEmptyAlternatives_whenNoRecommendationJob() {
		// Case 8: AI 추천을 거치지 않고 만들어진 Course(예: 시드 데이터)
		Course course = courseWithId(301L, "대전 가족 여행", LocalDate.now().plusDays(6));
		stubBaseCourse(course);
		stubNoTagsOrSchedule(course);
		given(recommendationCandidateRepository.findAllByJobCourseIdIn(List.of(301L))).willReturn(List.of());

		HomeResponse response = homeService.getHome(1L);

		assertThat(response.familyCourses().get(0).alternativeCandidates()).isEmpty();
		verify(recommendationCandidateItemRepository, never()).findAllWithPlaceByRecommendationCandidateIdIn(anyList());
	}

	@Test
	void getHome_returnsEmptyAlternatives_whenJobHasNoSelectedCandidate() {
		// Case 10: candidate는 있으나 selected=true 가 하나도 없음(정상 확정 흐름에서는 발생하지 않지만
		// 방어적으로 빈 배열 처리)
		Course course = courseWithId(301L, "대전 가족 여행", LocalDate.now().plusDays(6));
		RecommendationJob job = jobWithId(701L, course);
		RecommendationCandidate a = candidateWithId(1001L, job, 1, false, "A", "요약");
		RecommendationCandidate b = candidateWithId(1002L, job, 2, false, "B", "요약");

		stubBaseCourse(course);
		stubNoTagsOrSchedule(course);
		given(recommendationCandidateRepository.findAllByJobCourseIdIn(List.of(301L))).willReturn(List.of(a, b));

		HomeResponse response = homeService.getHome(1L);

		assertThat(response.familyCourses().get(0).alternativeCandidates()).isEmpty();
		verify(recommendationCandidateItemRepository, never()).findAllWithPlaceByRecommendationCandidateIdIn(anyList());
	}

	@Test
	void getHome_includesCourseInFamilyCourses_evenWithNoAlternatives() {
		// Case 11
		Course course = courseWithId(301L, "대전 가족 여행", LocalDate.now().plusDays(6));
		stubBaseCourse(course);
		stubNoAlternatives(course);

		HomeResponse response = homeService.getHome(1L);

		assertThat(response.familyCourses()).hasSize(1);
		assertThat(response.familyCourses().get(0).courseId()).isEqualTo(301L);
		assertThat(response.familyCourses().get(0).alternativeCandidates()).isEmpty();
	}

	@Test
	void getHome_doesNotMixAlternativeCandidates_acrossCourses() {
		// Case 12, 13: Course A/B 각각 자기 확정 job의 alternative만 받아야 한다.
		Course courseA = courseWithId(301L, "대전 가족 여행", LocalDate.now().plusDays(3));
		Course courseB = courseWithId(302L, "부산 가족 여행", LocalDate.now().plusDays(10));

		RecommendationJob jobA = jobWithId(701L, courseA);
		RecommendationCandidate a1 = candidateWithId(1001L, jobA, 1, false, "A1", "요약");
		RecommendationCandidate a2Selected = candidateWithId(1002L, jobA, 2, true, "A2", "요약");

		RecommendationJob jobB = jobWithId(702L, courseB);
		RecommendationCandidate b1Selected = candidateWithId(2001L, jobB, 1, true, "B1", "요약");
		RecommendationCandidate b2 = candidateWithId(2002L, jobB, 2, false, "B2", "요약");
		RecommendationCandidate b3 = candidateWithId(2003L, jobB, 3, false, "B3", "요약");

		given(courseRepository.findUpcomingCoursesForUser(1L)).willReturn(List.of(courseA, courseB));
		given(courseKeywordRepository.findAllByCourseIdIn(List.of(301L, 302L))).willReturn(List.of());
		given(courseScheduleItemRepository.findAllByCourseIdIn(List.of(301L, 302L))).willReturn(List.of());
		given(recommendationCandidateRepository.findAllByJobCourseIdIn(List.of(301L, 302L)))
			.willReturn(List.of(a1, a2Selected, b1Selected, b2, b3));
		given(recommendationCandidateItemRepository.findAllWithPlaceByRecommendationCandidateIdIn(List.of(1001L, 2002L, 2003L)))
			.willReturn(List.of());

		HomeResponse response = homeService.getHome(1L);

		HomeResponse.FamilyCourseResponse responseA = response.familyCourses().stream()
			.filter(course -> course.courseId().equals(301L)).findFirst().orElseThrow();
		HomeResponse.FamilyCourseResponse responseB = response.familyCourses().stream()
			.filter(course -> course.courseId().equals(302L)).findFirst().orElseThrow();

		assertThat(responseA.alternativeCandidates())
			.extracting(HomeResponse.AlternativeCandidateResponse::candidateId)
			.containsExactly(1001L);
		assertThat(responseB.alternativeCandidates())
			.extracting(HomeResponse.AlternativeCandidateResponse::candidateId)
			.containsExactly(2002L, 2003L);
	}

	@Test
	void getHome_doesNotFail_whenMultipleJobsHaveSelectedCandidate() {
		// 정상 흐름에서는 Course.confirm()이 평생 1회만 확정을 허용해 발생할 수 없는 데이터지만,
		// 방어적으로 "먼저 만난 job"만 채택하고 예외 없이 처리되는지 확인한다.
		Course course = courseWithId(301L, "대전 가족 여행", LocalDate.now().plusDays(6));
		RecommendationJob earlierJob = jobWithId(701L, course);
		RecommendationCandidate earlierSelected = candidateWithId(1001L, earlierJob, 1, true, "먼저 선택", "요약");
		RecommendationCandidate earlierAlt = candidateWithId(1002L, earlierJob, 2, false, "먼저 대안", "요약");

		RecommendationJob laterJob = jobWithId(702L, course);
		RecommendationCandidate laterSelected = candidateWithId(2001L, laterJob, 1, true, "나중 선택", "요약");

		stubBaseCourse(course);
		stubNoTagsOrSchedule(course);
		given(recommendationCandidateRepository.findAllByJobCourseIdIn(List.of(301L)))
			.willReturn(List.of(earlierSelected, earlierAlt, laterSelected));
		given(recommendationCandidateItemRepository.findAllWithPlaceByRecommendationCandidateIdIn(List.of(1002L)))
			.willReturn(List.of());

		HomeResponse response = homeService.getHome(1L);

		assertThat(response.familyCourses().get(0).alternativeCandidates())
			.extracting(HomeResponse.AlternativeCandidateResponse::candidateId)
			.containsExactly(1002L);
	}

	// ---- fixtures ----

	private void stubBaseCourse(Course course) {
		given(courseRepository.findUpcomingCoursesForUser(1L)).willReturn(List.of(course));
	}

	private void stubNoTagsOrSchedule(Course course) {
		Long courseId = course.getCourseId();
		given(courseKeywordRepository.findAllByCourseIdIn(List.of(courseId))).willReturn(List.of());
		given(courseScheduleItemRepository.findAllByCourseIdIn(List.of(courseId))).willReturn(List.of());
	}

	private void stubNoAlternatives(Course course) {
		stubNoTagsOrSchedule(course);
		given(recommendationCandidateRepository.findAllByJobCourseIdIn(List.of(course.getCourseId())))
			.willReturn(List.of());
	}

	private Course courseWithId(Long courseId, String title, LocalDate startDate) {
		User creator = User.create("KAKAO", "creator", "작성자", null);
		Region region = Region.create("3", "1", "대전광역시 동구");
		Course course = Course.create(creator, region, title, startDate, startDate.plusDays(1));
		ReflectionTestUtils.setField(course, "courseId", courseId);
		return course;
	}

	private RecommendationJob jobWithId(Long jobId, Course course) {
		RecommendationJob job = RecommendationJob.createPending(course);
		ReflectionTestUtils.setField(job, "recommendationJobId", jobId);
		return job;
	}

	private RecommendationCandidate candidateWithId(
		Long candidateId,
		RecommendationJob job,
		int rank,
		boolean selected,
		String title,
		String summary
	) {
		RecommendationCandidate candidate = RecommendationCandidate.create(
			job, rank, title, summary, new BigDecimal("90.0"), null, 60
		);
		ReflectionTestUtils.setField(candidate, "recommendationCandidateId", candidateId);
		if (selected) {
			candidate.select();
		}
		return candidate;
	}

	private RecommendationCandidateItem candidateItem(
		Long itemId,
		RecommendationCandidate candidate,
		Place place,
		int dayNumber,
		int visitOrder
	) {
		RecommendationCandidateItem item = RecommendationCandidateItem.create(
			candidate, place, dayNumber, visitOrder, null, null, null, null, null
		);
		ReflectionTestUtils.setField(item, "candidateItemId", itemId);
		return item;
	}

	private CourseKeyword courseKeyword(Course course, String code, String name) {
		CourseInterestKeyword keyword = CourseInterestKeyword.create(code, name);
		ReflectionTestUtils.setField(keyword, "keywordId", (long) name.hashCode());
		return CourseKeyword.create(course, keyword);
	}

	private CourseScheduleItem scheduleItem(Course course, Place place, int dayNumber, int visitOrder) {
		return CourseScheduleItem.create(course, place, dayNumber, visitOrder, null, null, null, null, null);
	}

	private Place placeWithImageUrl(Long placeId, String imageUrl) {
		Place place = Place.create(
			"CONTENT-" + placeId,
			"12",
			"KAKAO",
			"TOURIST_ATTRACTION",
			Region.create("3", "1", "대전광역시 동구"),
			"장소" + placeId,
			"대전광역시 서구 어딘가",
			new BigDecimal("36.366"),
			new BigDecimal("127.388"),
			imageUrl
		);
		ReflectionTestUtils.setField(place, "placeId", placeId);
		return place;
	}
}
