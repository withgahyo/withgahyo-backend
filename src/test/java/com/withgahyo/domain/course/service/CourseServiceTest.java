package com.withgahyo.domain.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.withgahyo.domain.course.dto.CreateCourseRequest;
import com.withgahyo.domain.course.dto.CreateCourseResponse;
import com.withgahyo.domain.course.entity.Course;
import com.withgahyo.domain.course.entity.CourseInterestKeyword;
import com.withgahyo.domain.course.entity.CourseStatus;
import com.withgahyo.domain.course.entity.TransportMode;
import com.withgahyo.domain.course.exception.CourseErrorCode;
import com.withgahyo.domain.course.repository.CourseInterestKeywordRepository;
import com.withgahyo.domain.course.repository.CourseKeywordRepository;
import com.withgahyo.domain.course.repository.CourseMustVisitPlaceRepository;
import com.withgahyo.domain.course.repository.CourseParticipantRepository;
import com.withgahyo.domain.course.repository.CourseRepository;
import com.withgahyo.domain.family.entity.FamilyRelation;
import com.withgahyo.domain.family.repository.FamilyRelationRepository;
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
	void createDraftCourse_fail_whenMustVisitPlaceIsOutsideSelectedRegion() {
		User creator = User.create("KAKAO", "creator", "작성자", null);
		Region courseRegion = Region.create("3", "1", "대전광역시 동구");
		Region otherRegion = Region.create("1", "1", "서울특별시 종로구");
		Place place = Place.create(
			"126508",
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
			.isEqualTo(CourseErrorCode.PLACE_OUT_OF_SELECTED_REGION);
	}
}
