package com.withgahyo.domain.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.withgahyo.domain.album.repository.AlbumRepository;
import com.withgahyo.domain.course.dto.CourseConfirmResponse;
import com.withgahyo.domain.course.dto.CourseDetailResponse;
import com.withgahyo.domain.course.dto.CourseLikeResponse;
import com.withgahyo.domain.course.dto.CreateCourseRequest;
import com.withgahyo.domain.course.dto.CreateCourseResponse;
import com.withgahyo.domain.course.dto.UpdateCourseRequest;
import com.withgahyo.domain.course.dto.UpdateCourseResponse;
import com.withgahyo.domain.course.entity.Course;
import com.withgahyo.domain.course.entity.CourseInterestKeyword;
import com.withgahyo.domain.course.entity.CourseLikeId;
import com.withgahyo.domain.course.entity.CourseMustVisitPlace;
import com.withgahyo.domain.course.entity.CourseStatus;
import com.withgahyo.domain.course.entity.TransportMode;
import com.withgahyo.domain.course.exception.CourseErrorCode;
import com.withgahyo.domain.course.repository.CourseInterestKeywordRepository;
import com.withgahyo.domain.course.repository.CourseKeywordRepository;
import com.withgahyo.domain.course.repository.CourseLikeRepository;
import com.withgahyo.domain.course.repository.CourseMustVisitPlaceRepository;
import com.withgahyo.domain.course.repository.CourseParticipantRepository;
import com.withgahyo.domain.course.repository.CourseRepository;
import com.withgahyo.domain.course.repository.CourseScheduleItemRepository;
import com.withgahyo.domain.family.entity.FamilyRelation;
import com.withgahyo.domain.family.repository.FamilyRelationRepository;
import com.withgahyo.domain.place.repository.PlaceAccessibilityRepository;
import com.withgahyo.domain.place.entity.Place;
import com.withgahyo.domain.place.entity.Region;
import com.withgahyo.domain.place.repository.PlaceRepository;
import com.withgahyo.domain.place.repository.RegionRepository;
import com.withgahyo.domain.user.entity.User;
import com.withgahyo.domain.user.repository.UserRepository;
import com.withgahyo.global.exception.BusinessException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

	@Mock
	private CourseRepository courseRepository;

	@Mock
	private CourseParticipantRepository courseParticipantRepository;

	@Mock
	private CourseKeywordRepository courseKeywordRepository;

	@Mock
	private CourseMustVisitPlaceRepository courseMustVisitPlaceRepository;

	@Mock
	private CourseInterestKeywordRepository courseInterestKeywordRepository;

	@Mock
	private CourseLikeRepository courseLikeRepository;

	@Mock
	private CourseScheduleItemRepository courseScheduleItemRepository;

	@Mock
	private AlbumRepository albumRepository;

	@Mock
	private PlaceAccessibilityRepository placeAccessibilityRepository;

	@Mock
	private FamilyRelationRepository familyRelationRepository;

	@Mock
	private PlaceRepository placeRepository;

	@Mock
	private RegionRepository regionRepository;

	@Mock
	private UserRepository userRepository;

	private CourseService courseService;

	@BeforeEach
	void setUp() {
		courseService = new CourseService(
			courseRepository,
			courseParticipantRepository,
			courseKeywordRepository,
			courseMustVisitPlaceRepository,
			courseInterestKeywordRepository,
			courseLikeRepository,
			courseScheduleItemRepository,
			albumRepository,
			placeAccessibilityRepository,
			familyRelationRepository,
			placeRepository,
			regionRepository,
			userRepository
		);
	}

	@Test
	void createDraftCourse_success() {
		User creator = User.create("KAKAO", "creator", "작성자", null);
		User familyUser = User.create("KAKAO", "family", "엄마", "https://example.com/mom.png");
		Region region = Region.create("3", "1", "대전광역시 동구");
		FamilyRelation relation = FamilyRelation.create(creator, familyUser, "엄마");
		CourseInterestKeyword keyword = CourseInterestKeyword.create("NATURE", "자연");
		Place place = Place.create(
			"126508",
			"12",
			"TOUR_API",
			"NATURE",
			region,
			"한밭수목원",
			"대전광역시 서구 둔산대로 169",
			null,
			null,
			"https://example.com/place.jpg"
		);

		given(userRepository.findById(1L)).willReturn(Optional.of(creator));
		given(regionRepository.findByAreaCodeAndSigunguCode("3", "1")).willReturn(Optional.of(region));
		given(familyRelationRepository.findActiveRelationsByUserIdAndFamilyUserIds(1L, List.of(2L)))
			.willReturn(List.of(relation));
		given(courseInterestKeywordRepository.findAllByKeywordIdInAndActiveIsTrue(List.of(10L)))
			.willReturn(List.of(keyword));
		given(placeRepository.findAllByPlaceIdIn(List.of(501L))).willReturn(List.of(place));
		given(courseRepository.save(org.mockito.ArgumentMatchers.any(Course.class)))
			.willAnswer(invocation -> invocation.getArgument(0));

		CreateCourseRequest request = new CreateCourseRequest(
			"대전 효도 여행",
			"3",
			"1",
			LocalDate.now().plusDays(1),
			LocalDate.now().plusDays(2),
			List.of(2L),
			List.of(10L),
			List.of(501L),
			TransportMode.CAR
		);

		CreateCourseResponse response = courseService.createDraftCourse(1L, request);

		assertThat(response.title()).isEqualTo("대전 효도 여행");
		assertThat(response.status()).isEqualTo(CourseStatus.DRAFT);
		ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
		verify(courseRepository).save(courseCaptor.capture());
		assertThat(courseCaptor.getValue().getRegion()).isEqualTo(region);
		assertThat(courseCaptor.getValue().getCreatorUser()).isEqualTo(creator);
		verify(courseParticipantRepository).saveAll(org.mockito.ArgumentMatchers.anyList());
		verify(courseKeywordRepository).saveAll(org.mockito.ArgumentMatchers.anyList());
		verify(courseMustVisitPlaceRepository).saveAll(org.mockito.ArgumentMatchers.anyList());
	}

	@Test
	void createDraftCourse_fail_whenFamilyMemberIsNotConnected() {
		User creator = User.create("KAKAO", "creator", "작성자", null);
		Region region = Region.create("3", "1", "대전광역시 동구");

		given(userRepository.findById(1L)).willReturn(Optional.of(creator));
		given(regionRepository.findByAreaCodeAndSigunguCode("3", "1")).willReturn(Optional.of(region));
		given(familyRelationRepository.findActiveRelationsByUserIdAndFamilyUserIds(1L, List.of(99L)))
			.willReturn(List.of());

		CreateCourseRequest request = new CreateCourseRequest(
			"대전 효도 여행",
			"3",
			"1",
			LocalDate.now().plusDays(1),
			LocalDate.now().plusDays(2),
			List.of(99L),
			List.of(),
			List.of(),
			TransportMode.CAR
		);

		assertThatThrownBy(() -> courseService.createDraftCourse(1L, request))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(CourseErrorCode.FAMILY_MEMBER_NOT_CONNECTED);
	}

	@Test
	void createDraftCourse_fail_whenStartDateIsBeforeToday() {
		CreateCourseRequest request = new CreateCourseRequest(
			"대전여행",
			"3",
			"0",
			LocalDate.now().minusDays(1),
			LocalDate.now().plusDays(1),
			List.of(),
			List.of(),
			List.of(),
			TransportMode.CAR
		);

		assertThatThrownBy(() -> courseService.createDraftCourse(1L, request))
			.isInstanceOf(BusinessException.class)
			.hasMessage("여행 시작일은 오늘 이상이고 종료일은 시작일 이후여야 합니다.");
	}

	@Test
	void createDraftCourse_savesMustVisitPlace_whenPlaceIsInSelectedRegion() {
		User creator = User.create("KAKAO", "creator", "작성자", null);
		Region region = Region.create("3", "1", "대전광역시 동구");
		Place place = Place.create(
			"126508",
			"12",
			"TOUR_API",
			"NATURE",
			region,
			"한밭수목원",
			"대전광역시 서구 둔산대로 169",
			null,
			null,
			null
		);

		given(userRepository.findById(1L)).willReturn(Optional.of(creator));
		given(regionRepository.findByAreaCodeAndSigunguCode("3", "1")).willReturn(Optional.of(region));
		given(placeRepository.findAllByPlaceIdIn(List.of(501L))).willReturn(List.of(place));
		given(courseRepository.save(org.mockito.ArgumentMatchers.any(Course.class)))
			.willAnswer(invocation -> invocation.getArgument(0));

		CreateCourseRequest request = new CreateCourseRequest(
			"대전 효도 여행",
			"3",
			"1",
			LocalDate.now().plusDays(1),
			LocalDate.now().plusDays(2),
			List.of(),
			List.of(),
			List.of(501L),
			TransportMode.CAR
		);

		courseService.createDraftCourse(1L, request);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<CourseMustVisitPlace>> captor = ArgumentCaptor.forClass(List.class);
		verify(courseMustVisitPlaceRepository).saveAll(captor.capture());
		assertThat(captor.getValue()).hasSize(1);
		assertThat(captor.getValue().get(0).getPlace()).isEqualTo(place);
	}

	@Test
	void createDraftCourse_fail_whenMustVisitPlaceIsOutsideSelectedRegion() {
		User creator = User.create("KAKAO", "creator", "작성자", null);
		Region courseRegion = Region.create("3", "1", "대전광역시 동구");
		Region otherRegion = Region.create("1", "1", "서울특별시 종로구");
		Place place = Place.create(
			"126508",
			"12",
			"TOUR_API",
			"NATURE",
			otherRegion,
			"경복궁",
			"서울특별시 종로구 사직로 161",
			null,
			null,
			null
		);

		given(userRepository.findById(1L)).willReturn(Optional.of(creator));
		given(regionRepository.findByAreaCodeAndSigunguCode("3", "1")).willReturn(Optional.of(courseRegion));
		given(placeRepository.findAllByPlaceIdIn(List.of(501L))).willReturn(List.of(place));

		CreateCourseRequest request = new CreateCourseRequest(
			"대전 효도 여행",
			"3",
			"1",
			LocalDate.now().plusDays(1),
			LocalDate.now().plusDays(2),
			List.of(),
			List.of(),
			List.of(501L),
			TransportMode.CAR
		);

		assertThatThrownBy(() -> courseService.createDraftCourse(1L, request))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(CourseErrorCode.MUST_VISIT_PLACE_REGION_MISMATCH);
		verify(courseRepository, never()).save(org.mockito.ArgumentMatchers.any(Course.class));
		verify(courseMustVisitPlaceRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
	}

	@Test
	void createDraftCourse_fail_whenMustVisitPlaceDoesNotExist() {
		User creator = User.create("KAKAO", "creator", "작성자", null);
		Region region = Region.create("3", "1", "대전광역시 동구");

		given(userRepository.findById(1L)).willReturn(Optional.of(creator));
		given(regionRepository.findByAreaCodeAndSigunguCode("3", "1")).willReturn(Optional.of(region));
		given(placeRepository.findAllByPlaceIdIn(List.of(999L))).willReturn(List.of());

		CreateCourseRequest request = new CreateCourseRequest(
			"대전 효도 여행",
			"3",
			"1",
			LocalDate.now().plusDays(1),
			LocalDate.now().plusDays(2),
			List.of(),
			List.of(),
			List.of(999L),
			TransportMode.CAR
		);

		assertThatThrownBy(() -> courseService.createDraftCourse(1L, request))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(CourseErrorCode.PLACE_NOT_FOUND);
		verify(courseRepository, never()).save(org.mockito.ArgumentMatchers.any(Course.class));
	}

	@Test
	void createDraftCourse_fail_whenAnyOfMultipleMustVisitPlacesIsOutsideSelectedRegion() {
		User creator = User.create("KAKAO", "creator", "작성자", null);
		Region courseRegion = Region.create("3", "1", "대전광역시 동구");
		Region otherRegion = Region.create("1", "1", "서울특별시 종로구");
		Place inRegionPlace = Place.create(
			"126508", "12", "TOUR_API", "NATURE", courseRegion,
			"한밭수목원", "대전광역시 서구 둔산대로 169", null, null, null
		);
		Place outOfRegionPlace = Place.create(
			"126509", "12", "TOUR_API", "HISTORY", otherRegion,
			"경복궁", "서울특별시 종로구 사직로 161", null, null, null
		);

		given(userRepository.findById(1L)).willReturn(Optional.of(creator));
		given(regionRepository.findByAreaCodeAndSigunguCode("3", "1")).willReturn(Optional.of(courseRegion));
		given(placeRepository.findAllByPlaceIdIn(List.of(501L, 502L)))
			.willReturn(List.of(inRegionPlace, outOfRegionPlace));

		CreateCourseRequest request = new CreateCourseRequest(
			"대전 효도 여행",
			"3",
			"1",
			LocalDate.now().plusDays(1),
			LocalDate.now().plusDays(2),
			List.of(),
			List.of(),
			List.of(501L, 502L),
			TransportMode.CAR
		);

		assertThatThrownBy(() -> courseService.createDraftCourse(1L, request))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(CourseErrorCode.MUST_VISIT_PLACE_REGION_MISMATCH);
		verify(courseRepository, never()).save(org.mockito.ArgumentMatchers.any(Course.class));
	}

	@Test
	void getCourseDetail_success() {
		User creator = userWithId(1L, "creator", "작성자", null);
		Region region = Region.create("3", "1", "대전광역시 동구");
		Course course = courseWithId(301L, creator, region, "대전 효도 여행");

		given(courseRepository.findActiveById(301L)).willReturn(Optional.of(course));
		given(courseParticipantRepository.findAllByCourseId(301L)).willReturn(List.of());
		given(courseKeywordRepository.findAllByCourseId(301L)).willReturn(List.of());
		given(courseLikeRepository.existsById(CourseLikeId.of(1L, 301L))).willReturn(true);
		given(courseLikeRepository.countByCourseCourseId(301L)).willReturn(3L);
		given(albumRepository.findFirstByCourseCourseIdOrderByAlbumIdAsc(301L)).willReturn(Optional.empty());
		given(courseScheduleItemRepository.findAllByCourseId(301L)).willReturn(List.of());

		CourseDetailResponse response = courseService.getCourseDetail(1L, 301L);

		assertThat(response.courseId()).isEqualTo(301L);
		assertThat(response.title()).isEqualTo("대전 효도 여행");
		assertThat(response.region().areaCode()).isEqualTo("3");
		assertThat(response.liked()).isTrue();
		assertThat(response.likeCount()).isEqualTo(3L);
	}

	@Test
	void likeCourse_success_withIdempotentInsert() {
		User user = userWithId(1L, "user", "사용자", null);
		Course course = courseWithId(301L, user, Region.create("3", "1", "대전광역시 동구"), "대전 효도 여행");

		given(courseRepository.findActiveById(301L)).willReturn(Optional.of(course));

		CourseLikeResponse response = courseService.likeCourse(1L, 301L);

		assertThat(response.liked()).isTrue();
		verify(courseLikeRepository).insertIgnore(1L, 301L);
	}

	@Test
	void unlikeCourse_success_withIdempotentDelete() {
		User user = userWithId(1L, "user", "사용자", null);
		Course course = courseWithId(301L, user, Region.create("3", "1", "대전광역시 동구"), "대전 효도 여행");
		CourseLikeId likeId = CourseLikeId.of(1L, 301L);

		given(courseRepository.findActiveById(301L)).willReturn(Optional.of(course));

		CourseLikeResponse response = courseService.unlikeCourse(1L, 301L);

		assertThat(response.liked()).isFalse();
		verify(courseLikeRepository).deleteByUserIdAndCourseId(1L, 301L);
		verify(courseLikeRepository, never()).deleteById(likeId);
	}

	@Test
	void confirmCourse_success() {
		User creator = userWithId(1L, "creator", "작성자", null);
		Course course = courseWithId(301L, creator, Region.create("3", "1", "대전광역시 동구"), "대전 효도 여행");

		given(courseRepository.findActiveById(301L)).willReturn(Optional.of(course));

		CourseConfirmResponse response = courseService.confirmCourse(1L, 301L);

		assertThat(response.confirmed()).isTrue();
		assertThat(course.getStatus()).isEqualTo(CourseStatus.UPCOMING);
		assertThat(course.getConfirmedAt()).isNotNull();
	}

	@Test
	void updateCourseBasicInfo_success() {
		User creator = userWithId(1L, "creator", "작성자", null);
		Course course = courseWithId(301L, creator, Region.create("3", "1", "대전광역시 동구"), "대전 효도 여행");

		given(courseRepository.findActiveById(301L)).willReturn(Optional.of(course));

		UpdateCourseResponse response = courseService.updateCourseBasicInfo(1L, 301L, new UpdateCourseRequest(" 대전 힐링 여행 "));

		assertThat(response.title()).isEqualTo("대전 힐링 여행");
		assertThat(response.updatedAt()).isNotNull();
		assertThat(course.getTitle()).isEqualTo("대전 힐링 여행");
	}

	@Test
	void updateCourseBasicInfo_fail_whenRequestHasNoValue() {
		assertThatThrownBy(() -> courseService.updateCourseBasicInfo(1L, 301L, new UpdateCourseRequest(null)))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(CourseErrorCode.COURSE_UPDATE_EMPTY);
	}

	@Test
	void updateCourseBasicInfo_fail_whenTitleIsBlankAfterTrim() {
		assertThatThrownBy(() -> courseService.updateCourseBasicInfo(1L, 301L, new UpdateCourseRequest("   ")))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(CourseErrorCode.INVALID_COURSE_TITLE);
	}

	@Test
	void deleteCourse_success_softDeletesCourse() {
		User creator = userWithId(1L, "creator", "작성자", null);
		Course course = courseWithId(301L, creator, Region.create("3", "1", "대전광역시 동구"), "대전 효도 여행");

		given(courseRepository.findActiveById(301L)).willReturn(Optional.of(course));

		courseService.deleteCourse(1L, 301L);

		assertThat(course.getDeletedAt()).isNotNull();
		assertThat(course.getStatus()).isEqualTo(CourseStatus.CANCELED);
	}

	private User userWithId(Long userId, String providerUserId, String nickname, String profileImageUrl) {
		User user = User.create("KAKAO", providerUserId, nickname, profileImageUrl);
		ReflectionTestUtils.setField(user, "userId", userId);
		return user;
	}

	private Course courseWithId(Long courseId, User creator, Region region, String title) {
		Course course = Course.create(creator, region, title, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));
		ReflectionTestUtils.setField(course, "courseId", courseId);
		return course;
	}
}
