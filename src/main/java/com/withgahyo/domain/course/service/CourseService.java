package com.withgahyo.domain.course.service;

import com.withgahyo.domain.album.entity.Album;
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
import com.withgahyo.domain.course.entity.CourseKeyword;
import com.withgahyo.domain.course.entity.CourseLikeId;
import com.withgahyo.domain.course.entity.CourseMustVisitPlace;
import com.withgahyo.domain.course.entity.CourseParticipant;
import com.withgahyo.domain.course.entity.CourseScheduleItem;
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
import com.withgahyo.domain.place.entity.AccessibilityStatus;
import com.withgahyo.domain.place.entity.Place;
import com.withgahyo.domain.place.entity.PlaceAccessibility;
import com.withgahyo.domain.place.entity.Region;
import com.withgahyo.domain.place.repository.PlaceAccessibilityRepository;
import com.withgahyo.domain.place.repository.PlaceRepository;
import com.withgahyo.domain.place.repository.RegionRepository;
import com.withgahyo.domain.user.entity.User;
import com.withgahyo.domain.user.repository.UserRepository;
import com.withgahyo.global.exception.BusinessException;
import com.withgahyo.global.exception.code.ErrorCode;
import com.withgahyo.global.exception.code.SecurityErrorCode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

	private final CourseRepository courseRepository;
	private final CourseParticipantRepository courseParticipantRepository;
	private final CourseKeywordRepository courseKeywordRepository;
	private final CourseMustVisitPlaceRepository courseMustVisitPlaceRepository;
	private final CourseInterestKeywordRepository courseInterestKeywordRepository;
	private final CourseLikeRepository courseLikeRepository;
	private final CourseScheduleItemRepository courseScheduleItemRepository;
	private final AlbumRepository albumRepository;
	private final PlaceAccessibilityRepository placeAccessibilityRepository;
	private final FamilyRelationRepository familyRelationRepository;
	private final PlaceRepository placeRepository;
	private final RegionRepository regionRepository;
	private final UserRepository userRepository;

	public CourseService(
		CourseRepository courseRepository,
		CourseParticipantRepository courseParticipantRepository,
		CourseKeywordRepository courseKeywordRepository,
		CourseMustVisitPlaceRepository courseMustVisitPlaceRepository,
		CourseInterestKeywordRepository courseInterestKeywordRepository,
		CourseLikeRepository courseLikeRepository,
		CourseScheduleItemRepository courseScheduleItemRepository,
		AlbumRepository albumRepository,
		PlaceAccessibilityRepository placeAccessibilityRepository,
		FamilyRelationRepository familyRelationRepository,
		PlaceRepository placeRepository,
		RegionRepository regionRepository,
		UserRepository userRepository
	) {
		this.courseRepository = courseRepository;
		this.courseParticipantRepository = courseParticipantRepository;
		this.courseKeywordRepository = courseKeywordRepository;
		this.courseMustVisitPlaceRepository = courseMustVisitPlaceRepository;
		this.courseInterestKeywordRepository = courseInterestKeywordRepository;
		this.courseLikeRepository = courseLikeRepository;
		this.courseScheduleItemRepository = courseScheduleItemRepository;
		this.albumRepository = albumRepository;
		this.placeAccessibilityRepository = placeAccessibilityRepository;
		this.familyRelationRepository = familyRelationRepository;
		this.placeRepository = placeRepository;
		this.regionRepository = regionRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public CreateCourseResponse createDraftCourse(Long userId, CreateCourseRequest request) {
		validatePeriod(request.startDate(), request.endDate());

		User creator = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(SecurityErrorCode.INVALID_TOKEN));
		Region region = regionRepository.findByAreaCodeAndSigunguCode(request.areaCode(), request.sigunguCode())
			.orElseThrow(() -> new BusinessException(CourseErrorCode.REGION_NOT_FOUND));

		List<FamilyRelation> familyRelations = findAndValidateFamilyRelations(userId, request.familyMemberIdsOrEmpty());
		List<CourseInterestKeyword> keywords = findAndValidateKeywords(request.keywordIdsOrEmpty());
		List<Place> mustVisitPlaces = findAndValidatePlaces(region, request.mustVisitPlaceIdsOrEmpty());

		Course course = courseRepository.save(Course.create(
			creator,
			region,
			request.title(),
			request.startDate(),
			request.endDate()
		));

		courseParticipantRepository.saveAll(familyRelations.stream()
			.map(relation -> CourseParticipant.create(course, relation.getFamilyUser(), relation.getRelationship()))
			.toList());
		courseKeywordRepository.saveAll(keywords.stream()
			.map(keyword -> CourseKeyword.create(course, keyword))
			.toList());
		courseMustVisitPlaceRepository.saveAll(mustVisitPlaces.stream()
			.map(place -> CourseMustVisitPlace.create(course, place))
			.toList());

		return CreateCourseResponse.from(course);
	}

	@Transactional(readOnly = true)
	public CourseDetailResponse getCourseDetail(Long userId, Long courseId) {
		Course course = findActiveCourse(courseId);
		validateOwnerOrParticipant(userId, course);

		List<CourseParticipant> participants = courseParticipantRepository.findAllByCourseId(courseId);
		List<CourseKeyword> keywords = courseKeywordRepository.findAllByCourseId(courseId);
		List<CourseScheduleItem> scheduleItems = courseScheduleItemRepository.findAllByCourseId(courseId);
		Map<Long, List<String>> accessibilitySummaries = findAccessibilitySummaries(scheduleItems);
		Long albumId = albumRepository.findFirstByCourseCourseIdOrderByAlbumIdAsc(courseId)
			.map(Album::getAlbumId)
			.orElse(null);

		return new CourseDetailResponse(
			course.getCourseId(),
			course.getTitle(),
			course.getStatus(),
			course.getStartDate(),
			course.getEndDate(),
			daysUntilTrip(course),
			new CourseDetailResponse.RegionResponse(
				course.getRegion().getAreaCode(),
				course.getRegion().getSigunguCode(),
				course.getRegion().getName()
			),
			course.getImageUrl(),
			keywords.stream()
				.map(keyword -> keyword.getKeyword().getName())
				.toList(),
			courseLikeRepository.existsById(CourseLikeId.of(userId, courseId)),
			courseLikeRepository.countByCourseCourseId(courseId),
			albumId,
			participants.stream()
				.map(participant -> new CourseDetailResponse.ParticipantResponse(
					participant.getUser().getUserId(),
					participant.getNameSnapshot(),
					participant.getRelationshipSnapshot(),
					participant.getProfileImageUrlSnapshot()
				))
				.toList(),
			toDayResponses(course, scheduleItems, accessibilitySummaries)
		);
	}

	@Transactional
	public CourseLikeResponse likeCourse(Long userId, Long courseId) {
		findActiveCourse(courseId);
		courseLikeRepository.insertIgnore(userId, courseId);
		return CourseLikeResponse.of(courseId, true);
	}

	@Transactional
	public CourseLikeResponse unlikeCourse(Long userId, Long courseId) {
		findActiveCourse(courseId);
		courseLikeRepository.deleteByUserIdAndCourseId(userId, courseId);
		return CourseLikeResponse.of(courseId, false);
	}

	@Transactional
	public CourseConfirmResponse confirmCourse(Long userId, Long courseId) {
		Course course = findActiveCourse(courseId);
		validateOwnerOrParticipant(userId, course);
		course.confirm();
		return CourseConfirmResponse.from(course);
	}

	@Transactional
	public UpdateCourseResponse updateCourseBasicInfo(Long userId, Long courseId, UpdateCourseRequest request) {
		if (request.hasNoValue()) {
			throw new BusinessException(CourseErrorCode.COURSE_UPDATE_EMPTY);
		}
		validateCourseTitle(request.normalizedTitle());
		Course course = findActiveCourse(courseId);
		validateOwnerOrParticipant(userId, course);
		course.updateBasicInfo(request.normalizedTitle());
		return UpdateCourseResponse.from(course);
	}

	@Transactional
	public void deleteCourse(Long userId, Long courseId) {
		Course course = findActiveCourse(courseId);
		validateOwnerOrParticipant(userId, course);
		course.softDelete();
	}

	private void validatePeriod(LocalDate startDate, LocalDate endDate) {
		LocalDate today = LocalDate.now();
		if (startDate.isBefore(today) || endDate.isBefore(startDate)) {
			throw new BusinessException(CourseErrorCode.INVALID_TRAVEL_PERIOD);
		}
	}

	private List<FamilyRelation> findAndValidateFamilyRelations(Long userId, List<Long> familyMemberIds) {
		return findAndValidate(
			familyMemberIds,
			ids -> familyRelationRepository.findActiveRelationsByUserIdAndFamilyUserIds(userId, ids),
			CourseErrorCode.FAMILY_MEMBER_NOT_CONNECTED
		);
	}

	private List<CourseInterestKeyword> findAndValidateKeywords(List<Long> keywordIds) {
		return findAndValidate(
			keywordIds,
			courseInterestKeywordRepository::findAllByKeywordIdInAndActiveIsTrue,
			CourseErrorCode.KEYWORD_NOT_FOUND
		);
	}

	private List<Place> findAndValidatePlaces(Region region, List<Long> placeIds) {
		List<Place> places = findAndValidate(
			placeIds,
			placeRepository::findAllByPlaceIdIn,
			CourseErrorCode.PLACE_NOT_FOUND
		);
		// 필수 방문 장소는 절대 조용히 제외하지 않는다. 하나라도 여행 지역과 다르면 요청 전체를 실패시킨다.
		if (places.stream().anyMatch(place -> !isInRegion(region, place))) {
			throw new BusinessException(CourseErrorCode.MUST_VISIT_PLACE_REGION_MISMATCH);
		}
		return places;
	}

	private <T> List<T> findAndValidate(
		List<Long> ids,
		Function<List<Long>, List<T>> finder,
		ErrorCode notFoundErrorCode
	) {
		if (ids.isEmpty()) {
			return List.of();
		}
		List<T> found = finder.apply(ids);
		if (found.size() != ids.stream().distinct().count()) {
			throw new BusinessException(notFoundErrorCode);
		}
		return found;
	}

	private boolean isInRegion(Region region, Place place) {
		return region.getAreaCode().equals(place.getRegion().getAreaCode())
			&& region.getSigunguCode().equals(place.getRegion().getSigunguCode());
	}

	private void validateCourseTitle(String title) {
		if (title.length() < 2 || title.length() > 30) {
			throw new BusinessException(CourseErrorCode.INVALID_COURSE_TITLE);
		}
	}

	private Course findActiveCourse(Long courseId) {
		return courseRepository.findActiveById(courseId)
			.orElseThrow(() -> new BusinessException(CourseErrorCode.COURSE_NOT_FOUND));
	}

	private void validateOwnerOrParticipant(Long userId, Course course) {
		if (course.getCreatorUser().getUserId().equals(userId)) {
			return;
		}
		if (courseParticipantRepository.existsByCourseIdAndUserId(course.getCourseId(), userId)) {
			return;
		}
		throw new BusinessException(CourseErrorCode.COURSE_ACCESS_DENIED);
	}

	private long daysUntilTrip(Course course) {
		return Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), course.getStartDate()));
	}

	private Map<Long, List<String>> findAccessibilitySummaries(List<CourseScheduleItem> scheduleItems) {
		List<Long> placeIds = scheduleItems.stream()
			.map(item -> item.getPlace().getPlaceId())
			.distinct()
			.toList();
		if (placeIds.isEmpty()) {
			return Map.of();
		}
		return placeAccessibilityRepository.findAllByPlaceIdsAndStatus(placeIds, AccessibilityStatus.AVAILABLE).stream()
			.collect(Collectors.groupingBy(
				accessibility -> accessibility.getPlace().getPlaceId(),
				Collectors.mapping(accessibility -> accessibility.getFacility().getName(), Collectors.toList())
			));
	}

	private List<CourseDetailResponse.DayResponse> toDayResponses(
		Course course,
		List<CourseScheduleItem> scheduleItems,
		Map<Long, List<String>> accessibilitySummaries
	) {
		return scheduleItems.stream()
			.collect(Collectors.groupingBy(CourseScheduleItem::getDayNumber))
			.entrySet()
			.stream()
			.sorted(Map.Entry.comparingByKey())
			.map(entry -> new CourseDetailResponse.DayResponse(
				entry.getKey(),
				course.getStartDate().plusDays(entry.getKey() - 1L),
				entry.getValue()
					.stream()
					.sorted(Comparator.comparing(CourseScheduleItem::getVisitOrder))
					.map(item -> toPlaceResponse(item, accessibilitySummaries))
					.toList()
			))
			.toList();
	}

	private CourseDetailResponse.PlaceResponse toPlaceResponse(
		CourseScheduleItem item,
		Map<Long, List<String>> accessibilitySummaries
	) {
		Place place = item.getPlace();
		return new CourseDetailResponse.PlaceResponse(
			item.getScheduleItemId(),
			item.getVisitOrder(),
			place.getPlaceId(),
			place.getName(),
			place.getCat1(),
			item.getArrivalTime(),
			item.getDepartureTime(),
			place.getLatitude(),
			place.getLongitude(),
			accessibilitySummaries.getOrDefault(place.getPlaceId(), List.of()),
			new CourseDetailResponse.TransportToNextResponse(
				item.getTransportModeToNext(),
				item.getDurationMinutesToNext(),
				item.getDistanceMetersToNext()
			)
		);
	}
}
