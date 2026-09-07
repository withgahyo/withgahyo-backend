package com.withgahyo.domain.recommendation.service;

import com.withgahyo.domain.album.entity.Album;
import com.withgahyo.domain.album.repository.AlbumRepository;
import com.withgahyo.domain.course.entity.Course;
import com.withgahyo.domain.course.entity.CourseKeyword;
import com.withgahyo.domain.course.entity.CourseMustVisitPlace;
import com.withgahyo.domain.course.entity.CourseParticipant;
import com.withgahyo.domain.course.entity.CourseScheduleItem;
import com.withgahyo.domain.course.entity.TransportMode;
import com.withgahyo.domain.course.repository.CourseKeywordRepository;
import com.withgahyo.domain.course.repository.CourseMustVisitPlaceRepository;
import com.withgahyo.domain.course.repository.CourseParticipantRepository;
import com.withgahyo.domain.course.repository.CourseRepository;
import com.withgahyo.domain.course.repository.CourseScheduleItemRepository;
import com.withgahyo.domain.place.entity.Place;
import com.withgahyo.domain.place.repository.PlaceRepository;
import com.withgahyo.domain.place.service.ExternalPlaceSearchResult;
import com.withgahyo.domain.place.service.PlaceUpsertWriter;
import com.withgahyo.domain.recommendation.dto.RecommendationCandidateDetailResponse;
import com.withgahyo.domain.recommendation.dto.RecommendationCandidatesResponse;
import com.withgahyo.domain.recommendation.dto.RecommendationStatusResponse;
import com.withgahyo.domain.recommendation.dto.SelectRecommendationCandidateRequest;
import com.withgahyo.domain.recommendation.dto.SelectRecommendationCandidateResponse;
import com.withgahyo.domain.recommendation.dto.StartRecommendationRequest;
import com.withgahyo.domain.recommendation.dto.StartRecommendationResponse;
import com.withgahyo.domain.recommendation.entity.RecommendationCandidate;
import com.withgahyo.domain.recommendation.entity.RecommendationCandidateItem;
import com.withgahyo.domain.recommendation.entity.RecommendationJob;
import com.withgahyo.domain.recommendation.entity.RecommendationJobStatus;
import com.withgahyo.domain.recommendation.exception.RecommendationErrorCode;
import com.withgahyo.domain.recommendation.repository.RecommendationCandidateItemRepository;
import com.withgahyo.domain.recommendation.repository.RecommendationCandidateRepository;
import com.withgahyo.domain.recommendation.repository.RecommendationJobRepository;
import com.withgahyo.domain.user.entity.User;
import com.withgahyo.domain.user.entity.UserOnboardingProfile;
import com.withgahyo.domain.user.repository.UserFacilityPreferenceRepository;
import com.withgahyo.domain.user.repository.UserFoodPreferenceRepository;
import com.withgahyo.domain.user.repository.UserOnboardingProfileRepository;
import com.withgahyo.domain.user.repository.UserTourismPreferenceRepository;
import com.withgahyo.global.exception.BusinessException;
import com.withgahyo.infra.ai.RecommendationAiClient;
import com.withgahyo.infra.ai.RecommendationAiException;
import com.withgahyo.infra.ai.dto.AiRecommendationGenerateRequest;
import com.withgahyo.infra.ai.dto.AiRecommendationGenerateResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RecommendationService {

	private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

	private final RecommendationJobRepository recommendationJobRepository;
	private final RecommendationCandidateRepository recommendationCandidateRepository;
	private final RecommendationCandidateItemRepository recommendationCandidateItemRepository;
	private final CourseRepository courseRepository;
	private final CourseParticipantRepository courseParticipantRepository;
	private final CourseKeywordRepository courseKeywordRepository;
	private final CourseMustVisitPlaceRepository courseMustVisitPlaceRepository;
	private final CourseScheduleItemRepository courseScheduleItemRepository;
	private final AlbumRepository albumRepository;
	private final UserOnboardingProfileRepository userOnboardingProfileRepository;
	private final UserTourismPreferenceRepository userTourismPreferenceRepository;
	private final UserFoodPreferenceRepository userFoodPreferenceRepository;
	private final UserFacilityPreferenceRepository userFacilityPreferenceRepository;
	private final PlaceRepository placeRepository;
	private final PlaceUpsertWriter placeUpsertWriter;
	private final RecommendationAiClient recommendationAiClient;

	public RecommendationService(
		RecommendationJobRepository recommendationJobRepository,
		RecommendationCandidateRepository recommendationCandidateRepository,
		RecommendationCandidateItemRepository recommendationCandidateItemRepository,
		CourseRepository courseRepository,
		CourseParticipantRepository courseParticipantRepository,
		CourseKeywordRepository courseKeywordRepository,
		CourseMustVisitPlaceRepository courseMustVisitPlaceRepository,
		CourseScheduleItemRepository courseScheduleItemRepository,
		AlbumRepository albumRepository,
		UserOnboardingProfileRepository userOnboardingProfileRepository,
		UserTourismPreferenceRepository userTourismPreferenceRepository,
		UserFoodPreferenceRepository userFoodPreferenceRepository,
		UserFacilityPreferenceRepository userFacilityPreferenceRepository,
		PlaceRepository placeRepository,
		PlaceUpsertWriter placeUpsertWriter,
		RecommendationAiClient recommendationAiClient
	) {
		this.recommendationJobRepository = recommendationJobRepository;
		this.recommendationCandidateRepository = recommendationCandidateRepository;
		this.recommendationCandidateItemRepository = recommendationCandidateItemRepository;
		this.courseRepository = courseRepository;
		this.courseParticipantRepository = courseParticipantRepository;
		this.courseKeywordRepository = courseKeywordRepository;
		this.courseMustVisitPlaceRepository = courseMustVisitPlaceRepository;
		this.courseScheduleItemRepository = courseScheduleItemRepository;
		this.albumRepository = albumRepository;
		this.userOnboardingProfileRepository = userOnboardingProfileRepository;
		this.userTourismPreferenceRepository = userTourismPreferenceRepository;
		this.userFoodPreferenceRepository = userFoodPreferenceRepository;
		this.userFacilityPreferenceRepository = userFacilityPreferenceRepository;
		this.placeRepository = placeRepository;
		this.placeUpsertWriter = placeUpsertWriter;
		this.recommendationAiClient = recommendationAiClient;
	}

	@Transactional
	public StartRecommendationResponse startGeneration(
		Long userId,
		Long courseId,
		StartRecommendationRequest request
	) {
		Course course = findCourse(courseId);
		validateCourseAccess(userId, course);
		// TODO(디버깅용, 원인 파악 후 제거)
		log.info(
			"[AI-DEBUG] course={}, creatorUser={}",
			course.getCourseId(),
			course.getCreatorUser().getUserId()
		);

		RecommendationJob job = recommendationJobRepository.save(RecommendationJob.createPending(course));
		job.start();
		try {
			AiRecommendationGenerateRequest aiRequest = toAiRequest(job, course, request);
			// TODO(디버깅용, 원인 파악 후 제거)
			log.info(
				"[AI-DEBUG] participants.size={}, participants={}",
				aiRequest.participants().size(),
				aiRequest.participants()
			);
			AiRecommendationGenerateResponse aiResponse = recommendationAiClient.generate(aiRequest);
			saveAiRecommendations(job, aiResponse);
			job.complete();
		} catch (RecommendationAiException exception) {
			job.fail(exception.getMessage());
		}
		return StartRecommendationResponse.from(job);
	}

	@Transactional(readOnly = true)
	public RecommendationStatusResponse getGenerationStatus(Long userId, Long generationId) {
		RecommendationJob job = findJob(generationId);
		validateCourseAccess(userId, job.getCourse());
		return RecommendationStatusResponse.from(job);
	}

	@Transactional(readOnly = true)
	public RecommendationCandidatesResponse getCandidates(Long userId, Long generationId) {
		RecommendationJob job = findCompletedJobForUser(userId, generationId);
		List<RecommendationCandidate> candidates =
			recommendationCandidateRepository.findAllByRecommendationJobRecommendationJobIdOrderByRankAsc(generationId);
		return RecommendationCandidatesResponse.of(job, candidates);
	}

	@Transactional(readOnly = true)
	public RecommendationCandidateDetailResponse getCandidateDetail(Long userId, Long generationId, Long candidateId) {
		findCompletedJobForUser(userId, generationId);
		RecommendationCandidate candidate = findCandidate(generationId, candidateId);
		List<RecommendationCandidateItem> items = findCandidateItems(candidateId);
		return RecommendationCandidateDetailResponse.of(candidate, items);
	}

	@Transactional
	public SelectRecommendationCandidateResponse selectCandidate(
		Long userId,
		Long generationId,
		SelectRecommendationCandidateRequest request
	) {
		RecommendationJob job = findCompletedJobForUser(userId, generationId);
		RecommendationCandidate candidate = findCandidate(generationId, request.candidateId());
		RecommendationCandidate selectedCandidate = recommendationCandidateRepository
			.findSelectedByRecommendationJobId(generationId)
			.orElse(null);

		if (selectedCandidate != null) {
			if (!selectedCandidate.getRecommendationCandidateId().equals(candidate.getRecommendationCandidateId())) {
				throw new BusinessException(RecommendationErrorCode.RECOMMENDATION_CANDIDATE_ALREADY_SELECTED);
			}
			Album album = findOrCreateAlbum(job.getCourse());
			return toSelectionResponse(job, selectedCandidate, album);
		}

		List<RecommendationCandidateItem> items = findCandidateItems(candidate.getRecommendationCandidateId());
		List<CourseScheduleItem> scheduleItems = items.stream()
			.map(item -> CourseScheduleItem.from(job.getCourse(), item))
			.toList();

		courseScheduleItemRepository.saveAll(scheduleItems);
		candidate.select();
		job.getCourse().confirm();
		Album album = findOrCreateAlbum(job.getCourse());

		return toSelectionResponse(job, candidate, album);
	}

	private RecommendationJob findCompletedJobForUser(Long userId, Long generationId) {
		RecommendationJob job = findJob(generationId);
		validateCourseAccess(userId, job.getCourse());
		if (job.getStatus() != RecommendationJobStatus.COMPLETED) {
			throw new BusinessException(RecommendationErrorCode.RECOMMENDATION_JOB_NOT_COMPLETED);
		}
		return job;
	}

	private RecommendationJob findJob(Long generationId) {
		return recommendationJobRepository.findWithCourseByRecommendationJobId(generationId)
			.orElseThrow(() -> new BusinessException(RecommendationErrorCode.RECOMMENDATION_JOB_NOT_FOUND));
	}

	private RecommendationCandidate findCandidate(Long generationId, Long candidateId) {
		return recommendationCandidateRepository
			.findByRecommendationCandidateIdAndRecommendationJobRecommendationJobId(candidateId, generationId)
			.orElseThrow(() -> new BusinessException(RecommendationErrorCode.RECOMMENDATION_CANDIDATE_NOT_FOUND));
	}

	private List<RecommendationCandidateItem> findCandidateItems(Long candidateId) {
		return recommendationCandidateItemRepository
			.findAllByRecommendationCandidateRecommendationCandidateIdOrderByDayNumberAscVisitOrderAsc(candidateId);
	}

	private Course findCourse(Long courseId) {
		Course course = courseRepository.findById(courseId)
			.orElseThrow(() -> new BusinessException(RecommendationErrorCode.COURSE_NOT_FOUND));
		if (course.isDeleted()) {
			throw new BusinessException(RecommendationErrorCode.COURSE_NOT_FOUND);
		}
		return course;
	}

	private void validateCourseAccess(Long userId, Course course) {
		if (course.getCreatorUser().getUserId().equals(userId)) {
			return;
		}
		if (courseParticipantRepository.existsByCourseIdAndUserId(course.getCourseId(), userId)) {
			return;
		}
		throw new BusinessException(RecommendationErrorCode.RECOMMENDATION_ACCESS_DENIED);
	}

	private Album findOrCreateAlbum(Course course) {
		return albumRepository.findFirstByCourseCourseIdOrderByAlbumIdAsc(course.getCourseId())
			.orElseGet(() -> albumRepository.save(Album.create(course)));
	}

	private AiRecommendationGenerateRequest toAiRequest(
		RecommendationJob job,
		Course course,
		StartRecommendationRequest request
	) {
		List<CourseKeyword> courseKeywords = courseKeywordRepository.findAllByCourseId(course.getCourseId());
		List<CourseMustVisitPlace> mustVisitPlaces = courseMustVisitPlaceRepository.findAllByCourseId(course.getCourseId());
		List<AiRecommendationGenerateRequest.ParticipantRequest> participants = toParticipants(course);

		return new AiRecommendationGenerateRequest(
			job.getRecommendationJobId(),
			course.getCourseId(),
			new AiRecommendationGenerateRequest.TripRequest(
				course.getRegion().getAreaCode(),
				course.getRegion().getSigunguCode(),
				course.getRegion().getName(),
				course.getStartDate(),
				course.getEndDate(),
				TransportMode.WALK,
				courseKeywords.stream()
					.map(courseKeyword -> courseKeyword.getKeyword().getCode())
					.toList(),
				mustVisitPlaces.stream()
					.map(courseMustVisitPlace -> courseMustVisitPlace.getPlace().getPlaceId())
					.toList(),
				request == null ? null : request.additionalRequest()
			),
			participants
		);
	}

	private List<AiRecommendationGenerateRequest.ParticipantRequest> toParticipants(Course course) {
		Map<Long, ParticipantSource> participantSources = new LinkedHashMap<>();
		User creator = course.getCreatorUser();
		participantSources.put(creator.getUserId(), new ParticipantSource(creator.getUserId(), "SELF"));
		List<CourseParticipant> courseParticipants = courseParticipantRepository.findAllByCourseId(course.getCourseId());
		// TODO(디버깅용, 원인 파악 후 제거)
		log.info(
			"[AI-DEBUG] courseParticipants.size={}, courseParticipantUserIds={}",
			courseParticipants.size(),
			courseParticipants.stream().map(cp -> cp.getUser().getUserId()).toList()
		);
		for (CourseParticipant participant : courseParticipants) {
			participantSources.putIfAbsent(
				participant.getUser().getUserId(),
				new ParticipantSource(participant.getUser().getUserId(), participant.getRelationshipSnapshot())
			);
		}

		List<Long> userIds = List.copyOf(participantSources.keySet());
		Map<Long, UserOnboardingProfile> profileByUserId = new LinkedHashMap<>();
		userOnboardingProfileRepository.findAllById(userIds)
			.forEach(profile -> profileByUserId.put(profile.getUserId(), profile));

		List<AiRecommendationGenerateRequest.ParticipantRequest> result = participantSources.values()
			.stream()
			.map(participant -> toParticipantRequest(participant, profileByUserId.get(participant.userId())))
			.toList();
		// TODO(디버깅용, 원인 파악 후 제거)
		log.info("[AI-DEBUG] toParticipants() result.size={}, userIds={}", result.size(), userIds);
		return result;
	}

	private AiRecommendationGenerateRequest.ParticipantRequest toParticipantRequest(
		ParticipantSource participant,
		UserOnboardingProfile profile
	) {
		return new AiRecommendationGenerateRequest.ParticipantRequest(
			participant.userId(),
			participant.relationship(),
			userTourismPreferenceRepository.findCodesByUserId(participant.userId()),
			userFoodPreferenceRepository.findCodesByUserId(participant.userId()),
			new AiRecommendationGenerateRequest.ConditionRequest(
				toMaxWalkingMinutes(profile),
				toNeedLevel(profile == null ? null : profile.getRestPreference()),
				toNeedLevel(profile == null ? null : profile.getStairsPreference()),
				toNeedLevel(profile == null ? null : profile.getSlopePreference()),
				userFacilityPreferenceRepository.findCodesByUserId(participant.userId()),
				toDietaryRestrictionCodes(profile)
			)
		);
	}

	private Integer toMaxWalkingMinutes(UserOnboardingProfile profile) {
		if (profile == null || profile.getWalkingTolerance() == null) {
			return 30;
		}
		String value = profile.getWalkingTolerance().toLowerCase();
		if (value.contains("10")) {
			return 10;
		}
		if (value.contains("1hour") || value.contains("60") || value.contains("over")) {
			return 60;
		}
		return 30;
	}

	private String toNeedLevel(String value) {
		if (value == null) {
			return "LOW";
		}
		String normalized = value.toLowerCase();
		if (normalized.contains("frequent") || normalized.contains("avoid") || normalized.contains("high")) {
			return "HIGH";
		}
		if (normalized.contains("moderate") || normalized.contains("medium")) {
			return "MEDIUM";
		}
		return "LOW";
	}

	// dietaryRestrictionCodes는 foodPreferenceCodes와 별개인 "식이 제한/주의사항" 개념으로 유지하되,
	// 현재 백엔드/AI 서버 양쪽 다 공식 코드셋이 없어 빈 배열로만 전달한다.
	// spicyPreference로 NO_SPICY/LOW_SPICY를 임의 생성하던 이전 로직은 합의되지 않은 코드라 제거했다.
	// TODO: 식이 제한 기능이 확정되면 전용 마스터/코드셋을 설계하고 이 메서드에서 매핑한다.
	private List<String> toDietaryRestrictionCodes(UserOnboardingProfile profile) {
		return List.of();
	}

	private void saveAiRecommendations(RecommendationJob job, AiRecommendationGenerateResponse aiResponse) {
		if (aiResponse == null || aiResponse.candidates() == null || aiResponse.candidates().isEmpty()) {
			throw new RecommendationAiException("AI 추천 후보가 없습니다.");
		}

		List<AiRecommendationGenerateResponse.ItemResponse> allItems = aiResponse.candidates()
			.stream()
			.flatMap(candidate -> candidate.days().stream())
			.flatMap(day -> day.items().stream())
			.toList();
		if (allItems.isEmpty()) {
			throw new RecommendationAiException("AI 추천 장소 목록이 비어 있습니다.");
		}

		Map<Long, Place> placeById = resolveKnownPlaces(allItems);
		Map<String, Place> placeByExternalKey = resolveOrCreateNewPlaces(allItems);
		// TODO(디버깅용, 원인 파악 후 제거)
		log.info(
			"[AI-DEBUG] resolved places: knownPlaceIds={}, newlyResolvedExternalKeys={}",
			placeById.keySet(),
			placeByExternalKey.keySet()
		);

		for (AiRecommendationGenerateResponse.CandidateResponse aiCandidate : aiResponse.candidates()) {
			RecommendationCandidate candidate = recommendationCandidateRepository.save(RecommendationCandidate.create(
				job,
				aiCandidate.rank(),
				aiCandidate.title(),
				aiCandidate.summary(),
				aiCandidate.score(),
				null,
				aiCandidate.totalEstimatedMinutes()
			));
			List<RecommendationCandidateItem> items = aiCandidate.days()
				.stream()
				.flatMap(day -> day.items()
					.stream()
					.map(item -> toCandidateItem(candidate, day.dayNumber(), item, placeById, placeByExternalKey)))
				.toList();
			recommendationCandidateItemRepository.saveAll(items);
		}
	}

	private Map<Long, Place> resolveKnownPlaces(List<AiRecommendationGenerateResponse.ItemResponse> items) {
		List<Long> placeIds = items.stream()
			.map(AiRecommendationGenerateResponse.ItemResponse::placeId)
			.filter(Objects::nonNull)
			.distinct()
			.toList();
		if (placeIds.isEmpty()) {
			return Map.of();
		}

		Map<Long, Place> placeById = placeRepository.findAllByPlaceIdIn(placeIds)
			.stream()
			.collect(Collectors.toMap(Place::getPlaceId, place -> place));
		if (placeById.size() != placeIds.size()) {
			throw new RecommendationAiException("AI 추천 장소를 찾을 수 없습니다.");
		}
		return placeById;
	}

	// placeId가 없는 item은 AI가 Spring place 테이블에 아직 없는 새 장소를 외부 식별 정보로 내려준 것이다.
	// 같은 (contentId, contentTypeId)가 여러 item/후보에 걸쳐 중복 등장해도 upsert는 한 번만 실행되도록
	// 전체 item 목록에서 먼저 중복을 제거한 뒤 PlaceUpsertWriter를 재사용한다.
	private Map<String, Place> resolveOrCreateNewPlaces(List<AiRecommendationGenerateResponse.ItemResponse> items) {
		List<AiRecommendationGenerateResponse.ItemResponse> newPlaceItems = items.stream()
			.filter(item -> item.placeId() == null)
			.toList();
		if (newPlaceItems.isEmpty()) {
			return Map.of();
		}

		Map<String, AiRecommendationGenerateResponse.ItemResponse> uniqueByExternalKey = new LinkedHashMap<>();
		for (AiRecommendationGenerateResponse.ItemResponse item : newPlaceItems) {
			String contentId = resolveContentId(item);
			String contentTypeId = item.contentTypeId();
			if (!StringUtils.hasText(contentId) || !StringUtils.hasText(contentTypeId)) {
				throw new RecommendationAiException("AI 추천 장소의 식별 정보(contentId/contentTypeId)가 부족합니다.");
			}
			uniqueByExternalKey.putIfAbsent(externalKey(contentId, contentTypeId), item);
		}

		List<ExternalPlaceSearchResult> results = uniqueByExternalKey.values()
			.stream()
			.map(this::toExternalPlaceSearchResult)
			.toList();
		List<Place> upsertedPlaces = placeUpsertWriter.upsertAll(results);

		Map<String, Place> placeByExternalKey = new LinkedHashMap<>();
		for (Place place : upsertedPlaces) {
			placeByExternalKey.put(externalKey(place.getContentId(), place.getContentTypeId()), place);
		}
		return placeByExternalKey;
	}

	private ExternalPlaceSearchResult toExternalPlaceSearchResult(AiRecommendationGenerateResponse.ItemResponse item) {
		return new ExternalPlaceSearchResult(
			item.source(),
			item.contentTypeId(),
			resolveContentId(item),
			item.name(),
			item.category(),
			item.address(),
			item.areaCode(),
			item.sigunguCode(),
			item.imageUrl(),
			item.latitude(),
			item.longitude()
		);
	}

	private String resolveContentId(AiRecommendationGenerateResponse.ItemResponse item) {
		return StringUtils.hasText(item.contentId()) ? item.contentId() : item.externalPlaceId();
	}

	private String externalKey(String contentId, String contentTypeId) {
		return contentId + "::" + contentTypeId;
	}

	private RecommendationCandidateItem toCandidateItem(
		RecommendationCandidate candidate,
		Integer dayNumber,
		AiRecommendationGenerateResponse.ItemResponse item,
		Map<Long, Place> placeById,
		Map<String, Place> placeByExternalKey
	) {
		Place place = resolvePlace(item, placeById, placeByExternalKey);
		return RecommendationCandidateItem.create(
			candidate,
			place,
			dayNumber,
			item.order(),
			null,
			null,
			null,
			null,
			null
		);
	}

	private Place resolvePlace(
		AiRecommendationGenerateResponse.ItemResponse item,
		Map<Long, Place> placeById,
		Map<String, Place> placeByExternalKey
	) {
		if (item.placeId() != null) {
			Place place = placeById.get(item.placeId());
			if (place == null) {
				throw new RecommendationAiException("AI 추천 장소를 찾을 수 없습니다.");
			}
			return place;
		}

		String contentId = resolveContentId(item);
		Place place = placeByExternalKey.get(externalKey(contentId, item.contentTypeId()));
		if (place == null) {
			throw new RecommendationAiException("AI 추천 장소를 찾을 수 없습니다.");
		}
		return place;
	}

	private SelectRecommendationCandidateResponse toSelectionResponse(
		RecommendationJob job,
		RecommendationCandidate candidate,
		Album album
	) {
		return new SelectRecommendationCandidateResponse(
			job.getCourse().getCourseId(),
			job.getRecommendationJobId(),
			candidate.getRecommendationCandidateId(),
			job.getCourse().getStatus(),
			album.getAlbumId(),
			job.getCourse().getConfirmedAt()
		);
	}

	private record ParticipantSource(Long userId, String relationship) {
	}
}
