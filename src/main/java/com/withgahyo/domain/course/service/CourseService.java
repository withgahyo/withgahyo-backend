package com.withgahyo.domain.course.service;

import com.withgahyo.domain.course.dto.CreateCourseRequest;
import com.withgahyo.domain.course.dto.CreateCourseResponse;
import com.withgahyo.domain.course.entity.Course;
import com.withgahyo.domain.course.entity.CourseInterestKeyword;
import com.withgahyo.domain.course.entity.CourseKeyword;
import com.withgahyo.domain.course.entity.CourseMustVisitPlace;
import com.withgahyo.domain.course.entity.CourseParticipant;
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
import com.withgahyo.global.exception.code.SecurityErrorCode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

	private final CourseRepository courseRepository;
	private final CourseParticipantRepository courseParticipantRepository;
	private final CourseKeywordRepository courseKeywordRepository;
	private final CourseMustVisitPlaceRepository courseMustVisitPlaceRepository;
	private final CourseInterestKeywordRepository courseInterestKeywordRepository;
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

	private void validatePeriod(LocalDate startDate, LocalDate endDate) {
		LocalDate today = LocalDate.now();
		if (startDate.isBefore(today) || endDate.isBefore(startDate)) {
			throw new BusinessException(CourseErrorCode.INVALID_TRAVEL_PERIOD);
		}
	}

	private List<FamilyRelation> findAndValidateFamilyRelations(Long userId, List<Long> familyMemberIds) {
		if (familyMemberIds.isEmpty()) {
			return List.of();
		}
		List<FamilyRelation> relations = familyRelationRepository.findActiveRelationsByUserIdAndFamilyUserIds(
			userId,
			familyMemberIds
		);
		if (relations.size() != familyMemberIds.stream().distinct().count()) {
			throw new BusinessException(CourseErrorCode.FAMILY_MEMBER_NOT_CONNECTED);
		}
		return relations;
	}

	private List<CourseInterestKeyword> findAndValidateKeywords(List<Long> keywordIds) {
		if (keywordIds.isEmpty()) {
			return List.of();
		}
		List<CourseInterestKeyword> keywords = courseInterestKeywordRepository.findAllByKeywordIdInAndActiveIsTrue(
			keywordIds
		);
		if (keywords.size() != keywordIds.stream().distinct().count()) {
			throw new BusinessException(CourseErrorCode.KEYWORD_NOT_FOUND);
		}
		return keywords;
	}

	private List<Place> findAndValidatePlaces(Region region, List<Long> placeIds) {
		if (placeIds.isEmpty()) {
			return List.of();
		}
		List<Place> places = placeRepository.findAllByPlaceIdIn(placeIds);
		if (places.size() != placeIds.stream().distinct().count()) {
			throw new BusinessException(CourseErrorCode.PLACE_NOT_FOUND);
		}

		places.forEach(place -> validatePlaceRegion(region, place));
		return places;
	}

	private void validatePlaceRegion(Region region, Place place) {
		if (!region.getAreaCode().equals(place.getRegion().getAreaCode())
			|| !region.getSigunguCode().equals(place.getRegion().getSigunguCode())) {
			throw new BusinessException(CourseErrorCode.PLACE_OUT_OF_SELECTED_REGION);
		}
	}
}
