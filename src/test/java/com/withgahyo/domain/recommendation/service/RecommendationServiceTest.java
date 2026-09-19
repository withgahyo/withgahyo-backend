package com.withgahyo.domain.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.withgahyo.domain.album.entity.Album;
import com.withgahyo.domain.album.repository.AlbumRepository;
import com.withgahyo.domain.course.entity.Course;
import com.withgahyo.domain.course.entity.CourseScheduleItem;
import com.withgahyo.domain.course.entity.CourseStatus;
import com.withgahyo.domain.course.entity.TransportMode;
import com.withgahyo.domain.course.repository.CourseParticipantRepository;
import com.withgahyo.domain.course.repository.CourseKeywordRepository;
import com.withgahyo.domain.course.repository.CourseMustVisitPlaceRepository;
import com.withgahyo.domain.course.repository.CourseRepository;
import com.withgahyo.domain.course.repository.CourseScheduleItemRepository;
import com.withgahyo.domain.place.entity.Place;
import com.withgahyo.domain.place.entity.Region;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
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
class RecommendationServiceTest {

	@Mock
	private RecommendationJobRepository recommendationJobRepository;

	@Mock
	private RecommendationCandidateRepository recommendationCandidateRepository;

	@Mock
	private RecommendationCandidateItemRepository recommendationCandidateItemRepository;

	@Mock
	private CourseRepository courseRepository;

	@Mock
	private CourseParticipantRepository courseParticipantRepository;

	@Mock
	private CourseKeywordRepository courseKeywordRepository;

	@Mock
	private CourseMustVisitPlaceRepository courseMustVisitPlaceRepository;

	@Mock
	private CourseScheduleItemRepository courseScheduleItemRepository;

	@Mock
	private AlbumRepository albumRepository;

	@Mock
	private UserOnboardingProfileRepository userOnboardingProfileRepository;

	@Mock
	private UserTourismPreferenceRepository userTourismPreferenceRepository;

	@Mock
	private UserFoodPreferenceRepository userFoodPreferenceRepository;

	@Mock
	private UserFacilityPreferenceRepository userFacilityPreferenceRepository;

	@Mock
	private PlaceRepository placeRepository;

	@Mock
	private PlaceUpsertWriter placeUpsertWriter;

	@Mock
	private RecommendationAiClient recommendationAiClient;

	private RecommendationService recommendationService;

	@BeforeEach
	void setUp() {
		recommendationService = new RecommendationService(
			recommendationJobRepository,
			recommendationCandidateRepository,
			recommendationCandidateItemRepository,
			courseRepository,
			courseParticipantRepository,
			courseKeywordRepository,
			courseMustVisitPlaceRepository,
			courseScheduleItemRepository,
			albumRepository,
			userOnboardingProfileRepository,
			userTourismPreferenceRepository,
			userFoodPreferenceRepository,
			userFacilityPreferenceRepository,
			placeRepository,
			placeUpsertWriter,
			recommendationAiClient
		);
	}

	@Test
	void startGeneration_success_callsAiServerAndStoresCandidates() {
		Course course = courseWithId(456L);
		RecommendationJob savedJob = RecommendationJob.createPending(course);
		ReflectionTestUtils.setField(savedJob, "recommendationJobId", 789L);
		Place place = placeWithId(501L);
		AiRecommendationGenerateResponse aiResponse = aiResponseWithPlace(789L, 456L, 501L);

		given(courseRepository.findById(456L)).willReturn(Optional.of(course));
		given(recommendationJobRepository.save(org.mockito.ArgumentMatchers.any(RecommendationJob.class)))
			.willReturn(savedJob);
		given(courseParticipantRepository.findAllByCourseId(456L)).willReturn(List.of());
		given(courseKeywordRepository.findAllByCourseId(456L)).willReturn(List.of());
		given(courseMustVisitPlaceRepository.findAllByCourseId(456L)).willReturn(List.of());
		given(userOnboardingProfileRepository.findAllById(List.of(1L))).willReturn(List.of());
		given(userTourismPreferenceRepository.findCodesByUserId(1L)).willReturn(List.of());
		given(userFoodPreferenceRepository.findCodesByUserId(1L)).willReturn(List.of());
		given(userFacilityPreferenceRepository.findCodesByUserId(1L)).willReturn(List.of());
		given(recommendationAiClient.generate(org.mockito.ArgumentMatchers.any(AiRecommendationGenerateRequest.class)))
			.willReturn(aiResponse);
		given(recommendationCandidateRepository.save(org.mockito.ArgumentMatchers.any(RecommendationCandidate.class)))
			.willAnswer(invocation -> invocation.getArgument(0));
		given(placeRepository.findAllByPlaceIdIn(List.of(501L))).willReturn(List.of(place));

		StartRecommendationResponse response = recommendationService.startGeneration(
			1L,
			456L,
			new StartRecommendationRequest(null, List.of(), null)
		);

		assertThat(response.generationId()).isEqualTo(789L);
		assertThat(response.courseId()).isEqualTo(456L);
		assertThat(response.status()).isEqualTo(RecommendationJobStatus.COMPLETED);
		ArgumentCaptor<AiRecommendationGenerateRequest> requestCaptor =
			ArgumentCaptor.forClass(AiRecommendationGenerateRequest.class);
		verify(recommendationAiClient).generate(requestCaptor.capture());
		assertThat(requestCaptor.getValue().generationId()).isEqualTo(789L);
		assertThat(requestCaptor.getValue().courseId()).isEqualTo(456L);
		assertThat(requestCaptor.getValue().participants()).hasSize(1);
		verify(recommendationCandidateRepository).save(org.mockito.ArgumentMatchers.any(RecommendationCandidate.class));
		verify(recommendationCandidateItemRepository).saveAll(org.mockito.ArgumentMatchers.anyList());
		// placeId가 이미 있는 경우 기존 조회 흐름만 타고, find-or-create 경로는 호출되지 않아야 한다.
		verify(placeUpsertWriter, never()).upsertAll(anyList());
	}

	@Test
	void startGeneration_success_sendsEmptyDietaryRestrictionCodes_regardlessOfSpicyPreference() {
		Course course = courseWithId(456L);
		RecommendationJob savedJob = RecommendationJob.createPending(course);
		ReflectionTestUtils.setField(savedJob, "recommendationJobId", 789L);
		Place place = placeWithId(501L);
		AiRecommendationGenerateResponse aiResponse = aiResponseWithPlace(789L, 456L, 501L);
		UserOnboardingProfile profile = UserOnboardingProfile.create(course.getCreatorUser());
		profile.updateConditions(null, null, null, null, "avoid");

		given(courseRepository.findById(456L)).willReturn(Optional.of(course));
		given(recommendationJobRepository.save(org.mockito.ArgumentMatchers.any(RecommendationJob.class)))
			.willReturn(savedJob);
		given(courseParticipantRepository.findAllByCourseId(456L)).willReturn(List.of());
		given(courseKeywordRepository.findAllByCourseId(456L)).willReturn(List.of());
		given(courseMustVisitPlaceRepository.findAllByCourseId(456L)).willReturn(List.of());
		given(userOnboardingProfileRepository.findAllById(List.of(1L))).willReturn(List.of(profile));
		given(userTourismPreferenceRepository.findCodesByUserId(1L)).willReturn(List.of());
		given(userFoodPreferenceRepository.findCodesByUserId(1L)).willReturn(List.of());
		given(userFacilityPreferenceRepository.findCodesByUserId(1L)).willReturn(List.of());
		given(recommendationAiClient.generate(org.mockito.ArgumentMatchers.any(AiRecommendationGenerateRequest.class)))
			.willReturn(aiResponse);
		given(recommendationCandidateRepository.save(org.mockito.ArgumentMatchers.any(RecommendationCandidate.class)))
			.willAnswer(invocation -> invocation.getArgument(0));
		given(placeRepository.findAllByPlaceIdIn(List.of(501L))).willReturn(List.of(place));

		recommendationService.startGeneration(1L, 456L, new StartRecommendationRequest(null, List.of(), null));

		ArgumentCaptor<AiRecommendationGenerateRequest> requestCaptor =
			ArgumentCaptor.forClass(AiRecommendationGenerateRequest.class);
		verify(recommendationAiClient).generate(requestCaptor.capture());
		assertThat(requestCaptor.getValue().participants().get(0).condition().dietaryRestrictionCodes()).isEmpty();
	}

	@Test
	void startGeneration_success_createsNewPlace_whenPlaceIdIsNull() {
		Course course = courseWithId(456L);
		RecommendationJob savedJob = RecommendationJob.createPending(course);
		ReflectionTestUtils.setField(savedJob, "recommendationJobId", 789L);
		AiRecommendationGenerateResponse.ItemResponse newItem = newPlaceItem("999999", "12", "새로운공원");
		AiRecommendationGenerateResponse aiResponse = singleCandidateResponse(789L, 456L, newItem);
		Place createdPlace = placeWithId(701L, "999999", "12");

		stubStartGenerationPrerequisites(course, savedJob, aiResponse);
		given(recommendationCandidateRepository.save(any(RecommendationCandidate.class)))
			.willAnswer(invocation -> invocation.getArgument(0));
		given(placeUpsertWriter.upsertAll(anyList())).willReturn(List.of(createdPlace));

		StartRecommendationResponse response = recommendationService.startGeneration(
			1L, 456L, new StartRecommendationRequest(null, List.of(), null)
		);

		assertThat(response.status()).isEqualTo(RecommendationJobStatus.COMPLETED);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<ExternalPlaceSearchResult>> upsertCaptor = ArgumentCaptor.forClass(List.class);
		verify(placeUpsertWriter).upsertAll(upsertCaptor.capture());
		assertThat(upsertCaptor.getValue()).hasSize(1);
		assertThat(upsertCaptor.getValue().get(0).externalPlaceId()).isEqualTo("999999");
		assertThat(upsertCaptor.getValue().get(0).contentTypeId()).isEqualTo("12");
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<RecommendationCandidateItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
		verify(recommendationCandidateItemRepository).saveAll(itemsCaptor.capture());
		assertThat(itemsCaptor.getValue()).hasSize(1);
		assertThat(itemsCaptor.getValue().get(0).getPlace()).isEqualTo(createdPlace);
	}

	@Test
	void startGeneration_success_reusesExistingPlace_whenPlaceIdIsNullButAlreadyUpserted() {
		// PlaceUpsertWriter는 findOrCreate이므로, 이미 존재하는 장소면 새 row 대신 기존 Place를 반환한다.
		// RecommendationService 입장에서는 반환된 Place를 그대로 재사용해서 연결하는지만 검증한다.
		Course course = courseWithId(456L);
		RecommendationJob savedJob = RecommendationJob.createPending(course);
		ReflectionTestUtils.setField(savedJob, "recommendationJobId", 789L);
		AiRecommendationGenerateResponse.ItemResponse newItem = newPlaceItem("126508", "12", "한밭수목원");
		AiRecommendationGenerateResponse aiResponse = singleCandidateResponse(789L, 456L, newItem);
		Place existingPlace = placeWithId(501L, "126508", "12");

		stubStartGenerationPrerequisites(course, savedJob, aiResponse);
		given(recommendationCandidateRepository.save(any(RecommendationCandidate.class)))
			.willAnswer(invocation -> invocation.getArgument(0));
		given(placeUpsertWriter.upsertAll(anyList())).willReturn(List.of(existingPlace));

		recommendationService.startGeneration(1L, 456L, new StartRecommendationRequest(null, List.of(), null));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<RecommendationCandidateItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
		verify(recommendationCandidateItemRepository).saveAll(itemsCaptor.capture());
		assertThat(itemsCaptor.getValue().get(0).getPlace().getPlaceId()).isEqualTo(501L);
	}

	@Test
	void startGeneration_success_upsertsOnce_whenSamePlaceAppearsInMultipleCandidates() {
		Course course = courseWithId(456L);
		RecommendationJob savedJob = RecommendationJob.createPending(course);
		ReflectionTestUtils.setField(savedJob, "recommendationJobId", 789L);
		AiRecommendationGenerateResponse.ItemResponse item = newPlaceItem("999999", "12", "새로운공원");
		AiRecommendationGenerateResponse.CandidateResponse candidate1 = new AiRecommendationGenerateResponse.CandidateResponse(
			1, "코스1", "요약1", "이유1", new BigDecimal("92.0"), 150,
			List.of(new AiRecommendationGenerateResponse.DayResponse(1, List.of(item))), List.of()
		);
		AiRecommendationGenerateResponse.CandidateResponse candidate2 = new AiRecommendationGenerateResponse.CandidateResponse(
			2, "코스2", "요약2", "이유2", new BigDecimal("88.0"), 140,
			List.of(new AiRecommendationGenerateResponse.DayResponse(1, List.of(item))), List.of()
		);
		AiRecommendationGenerateResponse aiResponse =
			new AiRecommendationGenerateResponse(789L, 456L, List.of(candidate1, candidate2));
		Place createdPlace = placeWithId(701L, "999999", "12");

		stubStartGenerationPrerequisites(course, savedJob, aiResponse);
		given(recommendationCandidateRepository.save(any(RecommendationCandidate.class)))
			.willAnswer(invocation -> invocation.getArgument(0));
		given(placeUpsertWriter.upsertAll(anyList())).willReturn(List.of(createdPlace));

		recommendationService.startGeneration(1L, 456L, new StartRecommendationRequest(null, List.of(), null));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<ExternalPlaceSearchResult>> upsertCaptor = ArgumentCaptor.forClass(List.class);
		verify(placeUpsertWriter).upsertAll(upsertCaptor.capture());
		assertThat(upsertCaptor.getValue()).hasSize(1);
		verify(recommendationCandidateRepository, org.mockito.Mockito.times(2))
			.save(any(RecommendationCandidate.class));
	}

	@Test
	void startGeneration_fail_whenNewPlaceItemHasNoIdentifyingInfo() {
		Course course = courseWithId(456L);
		RecommendationJob savedJob = RecommendationJob.createPending(course);
		ReflectionTestUtils.setField(savedJob, "recommendationJobId", 789L);
		AiRecommendationGenerateResponse.ItemResponse invalidItem = new AiRecommendationGenerateResponse.ItemResponse(
			1, "이름없는장소", "TOUR", "TOUR_API", null,
			null, null, "12", "주소", "3", "1",
			new BigDecimal("36.362"), new BigDecimal("127.459"), null,
			60, List.of()
		);
		AiRecommendationGenerateResponse aiResponse = singleCandidateResponse(789L, 456L, invalidItem);

		stubStartGenerationPrerequisites(course, savedJob, aiResponse);

		StartRecommendationResponse response = recommendationService.startGeneration(
			1L, 456L, new StartRecommendationRequest(null, List.of(), null)
		);

		assertThat(response.status()).isEqualTo(RecommendationJobStatus.FAILED);
		assertThat(savedJob.getErrorMessage()).isEqualTo("AI 추천 장소의 식별 정보(contentId/contentTypeId)가 부족합니다.");
		verify(placeUpsertWriter, never()).upsertAll(anyList());
	}

	@Test
	void startGeneration_backfillsPlaceImageUrl_whenExistingPlaceImageIsNullAndAiImageExists() {
		Place existingPlace = placeWithId(501L);
		ReflectionTestUtils.setField(existingPlace, "imageUrl", null);

		runStartGenerationWithKnownPlace(existingPlace, "https://tour.example.com/new.jpg");

		assertThat(existingPlace.getImageUrl()).isEqualTo("https://tour.example.com/new.jpg");
	}

	@Test
	void startGeneration_keepsExistingPlaceImageUrl_whenBothExistingAndAiImageExist() {
		Place existingPlace = placeWithId(501L);
		ReflectionTestUtils.setField(existingPlace, "imageUrl", "https://example.com/existing.jpg");

		runStartGenerationWithKnownPlace(existingPlace, "https://tour.example.com/new.jpg");

		assertThat(existingPlace.getImageUrl()).isEqualTo("https://example.com/existing.jpg");
	}

	@Test
	void startGeneration_keepsExistingPlaceImageUrl_whenAiImageIsNull() {
		Place existingPlace = placeWithId(501L);
		ReflectionTestUtils.setField(existingPlace, "imageUrl", "https://example.com/existing.jpg");

		runStartGenerationWithKnownPlace(existingPlace, null);

		assertThat(existingPlace.getImageUrl()).isEqualTo("https://example.com/existing.jpg");
	}

	@Test
	void startGeneration_keepsNullPlaceImageUrl_whenBothExistingAndAiImageAreNull() {
		Place existingPlace = placeWithId(501L);
		ReflectionTestUtils.setField(existingPlace, "imageUrl", null);

		runStartGenerationWithKnownPlace(existingPlace, null);

		assertThat(existingPlace.getImageUrl()).isNull();
	}

	@Test
	void startGeneration_backfillsPlaceImageUrl_whenExistingPlaceImageIsBlank() {
		// DB에 빈 문자열이 저장된 장소도 null 과 동일하게 "이미지 없음"으로 보고 보강한다.
		Place existingPlace = placeWithId(501L);
		ReflectionTestUtils.setField(existingPlace, "imageUrl", "   ");

		runStartGenerationWithKnownPlace(existingPlace, "https://tour.example.com/new.jpg");

		assertThat(existingPlace.getImageUrl()).isEqualTo("https://tour.example.com/new.jpg");
	}

	@Test
	void startGeneration_keepsExistingPlaceImageUrl_whenAiImageIsBlank() {
		Place existingPlace = placeWithId(501L);
		ReflectionTestUtils.setField(existingPlace, "imageUrl", "https://example.com/existing.jpg");

		runStartGenerationWithKnownPlace(existingPlace, "   ");

		assertThat(existingPlace.getImageUrl()).isEqualTo("https://example.com/existing.jpg");
	}

	@Test
	void startGeneration_keepsNullPlaceImageUrl_whenAiImageIsBlank() {
		Place existingPlace = placeWithId(501L);
		ReflectionTestUtils.setField(existingPlace, "imageUrl", null);

		runStartGenerationWithKnownPlace(existingPlace, "   ");

		assertThat(existingPlace.getImageUrl()).isNull();
	}

	@Test
	void startGeneration_doesNotChangeOtherPlaceFields_whenBackfillingImageUrl() {
		// imageUrl 보강이 기존 장소의 다른 정보를 덮어쓰지 않아야 한다.
		Place existingPlace = placeWithId(501L);
		ReflectionTestUtils.setField(existingPlace, "imageUrl", null);
		ReflectionTestUtils.setField(existingPlace, "name", "기존이름");
		ReflectionTestUtils.setField(existingPlace, "address", "기존주소");
		ReflectionTestUtils.setField(existingPlace, "cat1", "기존카테고리");

		runStartGenerationWithKnownPlace(existingPlace, "https://tour.example.com/new.jpg");

		assertThat(existingPlace.getImageUrl()).isEqualTo("https://tour.example.com/new.jpg");
		assertThat(existingPlace.getName()).isEqualTo("기존이름");
		assertThat(existingPlace.getAddress()).isEqualTo("기존주소");
		assertThat(existingPlace.getCat1()).isEqualTo("기존카테고리");
		assertThat(existingPlace.getContentId()).isEqualTo("126508");
		assertThat(existingPlace.getContentTypeId()).isEqualTo("12");
		assertThat(existingPlace.getSource()).isEqualTo("TOUR_API");
		assertThat(existingPlace.getLatitude()).isEqualByComparingTo("36.366");
		assertThat(existingPlace.getLongitude()).isEqualByComparingTo("127.388");
		// 기존 장소는 신규 등록 경로(upsert)를 타지 않는다.
		verify(placeUpsertWriter, never()).upsertAll(anyList());
	}

	private void runStartGenerationWithKnownPlace(Place existingPlace, String aiImageUrl) {
		Course course = courseWithId(456L);
		RecommendationJob savedJob = RecommendationJob.createPending(course);
		ReflectionTestUtils.setField(savedJob, "recommendationJobId", 789L);
		AiRecommendationGenerateResponse aiResponse = singleCandidateResponse(
			789L,
			456L,
			itemWithPlaceId(existingPlace.getPlaceId(), aiImageUrl)
		);

		stubStartGenerationPrerequisites(course, savedJob, aiResponse);
		given(recommendationCandidateRepository.save(any(RecommendationCandidate.class)))
			.willAnswer(invocation -> invocation.getArgument(0));
		given(placeRepository.findAllByPlaceIdIn(List.of(existingPlace.getPlaceId())))
			.willReturn(List.of(existingPlace));

		StartRecommendationResponse response = recommendationService.startGeneration(
			1L, 456L, new StartRecommendationRequest(null, List.of(), null)
		);

		assertThat(response.status()).isEqualTo(RecommendationJobStatus.COMPLETED);
	}

	private void stubStartGenerationPrerequisites(
		Course course,
		RecommendationJob savedJob,
		AiRecommendationGenerateResponse aiResponse
	) {
		given(courseRepository.findById(456L)).willReturn(Optional.of(course));
		given(recommendationJobRepository.save(any(RecommendationJob.class))).willReturn(savedJob);
		given(courseParticipantRepository.findAllByCourseId(456L)).willReturn(List.of());
		given(courseKeywordRepository.findAllByCourseId(456L)).willReturn(List.of());
		given(courseMustVisitPlaceRepository.findAllByCourseId(456L)).willReturn(List.of());
		given(userOnboardingProfileRepository.findAllById(List.of(1L))).willReturn(List.of());
		given(userTourismPreferenceRepository.findCodesByUserId(1L)).willReturn(List.of());
		given(userFoodPreferenceRepository.findCodesByUserId(1L)).willReturn(List.of());
		given(userFacilityPreferenceRepository.findCodesByUserId(1L)).willReturn(List.of());
		given(recommendationAiClient.generate(any(AiRecommendationGenerateRequest.class))).willReturn(aiResponse);
	}

	@Test
	void startGeneration_fail_whenAiServerCallFails_marksJobFailed() {
		Course course = courseWithId(456L);
		RecommendationJob savedJob = RecommendationJob.createPending(course);
		ReflectionTestUtils.setField(savedJob, "recommendationJobId", 789L);

		given(courseRepository.findById(456L)).willReturn(Optional.of(course));
		given(recommendationJobRepository.save(org.mockito.ArgumentMatchers.any(RecommendationJob.class)))
			.willReturn(savedJob);
		given(courseParticipantRepository.findAllByCourseId(456L)).willReturn(List.of());
		given(courseKeywordRepository.findAllByCourseId(456L)).willReturn(List.of());
		given(courseMustVisitPlaceRepository.findAllByCourseId(456L)).willReturn(List.of());
		given(userOnboardingProfileRepository.findAllById(List.of(1L))).willReturn(List.of());
		given(userTourismPreferenceRepository.findCodesByUserId(1L)).willReturn(List.of());
		given(userFoodPreferenceRepository.findCodesByUserId(1L)).willReturn(List.of());
		given(userFacilityPreferenceRepository.findCodesByUserId(1L)).willReturn(List.of());
		given(recommendationAiClient.generate(org.mockito.ArgumentMatchers.any(AiRecommendationGenerateRequest.class)))
			.willThrow(new RecommendationAiException("AI 서버 호출에 실패했습니다."));

		StartRecommendationResponse response = recommendationService.startGeneration(
			1L,
			456L,
			new StartRecommendationRequest(null, List.of(), null)
		);

		assertThat(response.status()).isEqualTo(RecommendationJobStatus.FAILED);
		assertThat(savedJob.getErrorMessage()).isEqualTo("AI 서버 호출에 실패했습니다.");
	}

	@Test
	void startGeneration_fail_whenUserCannotAccessCourse() {
		Course course = courseWithId(456L);

		given(courseRepository.findById(456L)).willReturn(Optional.of(course));
		given(courseParticipantRepository.existsByCourseIdAndUserId(456L, 99L)).willReturn(false);

		assertThatThrownBy(() -> recommendationService.startGeneration(99L, 456L, null))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(RecommendationErrorCode.RECOMMENDATION_ACCESS_DENIED);
	}

	@Test
	void getGenerationStatus_success() {
		Course course = courseWithId(456L);
		RecommendationJob job = RecommendationJob.createPending(course);
		ReflectionTestUtils.setField(job, "recommendationJobId", 789L);
		job.start();

		given(recommendationJobRepository.findWithCourseByRecommendationJobId(789L)).willReturn(Optional.of(job));

		RecommendationStatusResponse response = recommendationService.getGenerationStatus(1L, 789L);

		assertThat(response.generationId()).isEqualTo(789L);
		assertThat(response.courseId()).isEqualTo(456L);
		assertThat(response.status()).isEqualTo(RecommendationJobStatus.RUNNING);
		assertThat(response.startedAt()).isNotNull();
	}

	@Test
	void getCandidates_fail_whenJobIsNotCompleted() {
		Course course = courseWithId(456L);
		RecommendationJob job = RecommendationJob.createPending(course);
		ReflectionTestUtils.setField(job, "recommendationJobId", 789L);

		given(recommendationJobRepository.findWithCourseByRecommendationJobId(789L)).willReturn(Optional.of(job));

		assertThatThrownBy(() -> recommendationService.getCandidates(1L, 789L))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(RecommendationErrorCode.RECOMMENDATION_JOB_NOT_COMPLETED);
	}

	@Test
	void getCandidates_success_returnsSummaryList() {
		Course course = courseWithId(456L);
		RecommendationJob job = completedJobWithId(789L, course);
		RecommendationCandidate candidate = candidateWithId(1001L, job, 1);

		given(recommendationJobRepository.findWithCourseByRecommendationJobId(789L)).willReturn(Optional.of(job));
		given(recommendationCandidateRepository.findAllByRecommendationJobRecommendationJobIdOrderByRankAsc(789L))
			.willReturn(List.of(candidate));

		RecommendationCandidatesResponse response = recommendationService.getCandidates(1L, 789L);

		assertThat(response.generationId()).isEqualTo(789L);
		assertThat(response.candidates()).hasSize(1);
		assertThat(response.candidates().get(0).candidateId()).isEqualTo(1001L);
		assertThat(response.candidates().get(0).summary()).isEqualTo("이동 부담을 줄인 코스입니다.");
		assertThat(response.candidates().get(0).totalDistanceKm()).isEqualByComparingTo("12.4");
	}

	@Test
	void getCandidateDetail_success_returnsOrderedDays() {
		Course course = courseWithId(456L);
		RecommendationJob job = completedJobWithId(789L, course);
		RecommendationCandidate candidate = candidateWithId(1001L, job, 1);
		RecommendationCandidateItem item = candidateItemWithId(2001L, candidate, placeWithId(501L), 1, 1);

		given(recommendationJobRepository.findWithCourseByRecommendationJobId(789L)).willReturn(Optional.of(job));
		given(recommendationCandidateRepository.findByRecommendationCandidateIdAndRecommendationJobRecommendationJobId(1001L, 789L))
			.willReturn(Optional.of(candidate));
		given(recommendationCandidateItemRepository.findAllByRecommendationCandidateRecommendationCandidateIdOrderByDayNumberAscVisitOrderAsc(1001L))
			.willReturn(List.of(item));

		RecommendationCandidateDetailResponse response = recommendationService.getCandidateDetail(1L, 789L, 1001L);

		assertThat(response.candidateId()).isEqualTo(1001L);
		assertThat(response.days()).hasSize(1);
		assertThat(response.days().get(0).places().get(0).placeId()).isEqualTo(501L);
		assertThat(response.days().get(0).places().get(0).transportToNext().mode()).isEqualTo(TransportMode.CAR);
	}

	@Test
	void getCandidateDetail_returnsPlaceImageUrlAndAddress() {
		Course course = courseWithId(456L);
		RecommendationJob job = completedJobWithId(789L, course);
		RecommendationCandidate candidate = candidateWithId(1001L, job, 1);
		RecommendationCandidateItem item = candidateItemWithId(2001L, candidate, placeWithId(501L), 1, 1);

		given(recommendationJobRepository.findWithCourseByRecommendationJobId(789L)).willReturn(Optional.of(job));
		given(recommendationCandidateRepository.findByRecommendationCandidateIdAndRecommendationJobRecommendationJobId(1001L, 789L))
			.willReturn(Optional.of(candidate));
		given(recommendationCandidateItemRepository.findAllByRecommendationCandidateRecommendationCandidateIdOrderByDayNumberAscVisitOrderAsc(1001L))
			.willReturn(List.of(item));

		RecommendationCandidateDetailResponse response = recommendationService.getCandidateDetail(1L, 789L, 1001L);

		RecommendationCandidateDetailResponse.PlaceResponse placeResponse = response.days().get(0).places().get(0);
		assertThat(placeResponse.imageUrl()).isEqualTo("https://example.com/place.jpg");
		assertThat(placeResponse.address()).isEqualTo("대전광역시 서구 둔산대로 169");
	}

	@Test
	void getCandidateDetail_returnsNullImageUrl_whenPlaceHasNoImage() {
		// Kakao 기반 FOOD/CAFE 장소는 imageUrl 이 없다. null 이어도 응답이 정상 생성되어야 한다.
		Course course = courseWithId(456L);
		RecommendationJob job = completedJobWithId(789L, course);
		RecommendationCandidate candidate = candidateWithId(1001L, job, 1);
		Place placeWithoutImage = placeWithId(502L, "CONTENT-502", "39");
		ReflectionTestUtils.setField(placeWithoutImage, "imageUrl", null);
		RecommendationCandidateItem item = candidateItemWithId(2002L, candidate, placeWithoutImage, 1, 1);

		given(recommendationJobRepository.findWithCourseByRecommendationJobId(789L)).willReturn(Optional.of(job));
		given(recommendationCandidateRepository.findByRecommendationCandidateIdAndRecommendationJobRecommendationJobId(1001L, 789L))
			.willReturn(Optional.of(candidate));
		given(recommendationCandidateItemRepository.findAllByRecommendationCandidateRecommendationCandidateIdOrderByDayNumberAscVisitOrderAsc(1001L))
			.willReturn(List.of(item));

		RecommendationCandidateDetailResponse response = recommendationService.getCandidateDetail(1L, 789L, 1001L);

		RecommendationCandidateDetailResponse.PlaceResponse placeResponse = response.days().get(0).places().get(0);
		assertThat(placeResponse.imageUrl()).isNull();
		assertThat(placeResponse.address()).isEqualTo("대전광역시 서구 둔산대로 169");
		// 기존 필드는 그대로 유지된다.
		assertThat(placeResponse.placeId()).isEqualTo(502L);
		assertThat(placeResponse.name()).isEqualTo("한밭수목원");
	}

	@Test
	void selectCandidate_success_copiesScheduleAndCreatesAlbum() {
		Course course = courseWithId(456L);
		RecommendationJob job = completedJobWithId(789L, course);
		RecommendationCandidate candidate = candidateWithId(1001L, job, 1);
		RecommendationCandidateItem item = candidateItemWithId(2001L, candidate, placeWithId(501L), 1, 1);
		Album album = Album.create(course);
		ReflectionTestUtils.setField(album, "albumId", 3001L);

		given(recommendationJobRepository.findWithCourseByRecommendationJobId(789L)).willReturn(Optional.of(job));
		given(recommendationCandidateRepository.findByRecommendationCandidateIdAndRecommendationJobRecommendationJobId(1001L, 789L))
			.willReturn(Optional.of(candidate));
		given(recommendationCandidateRepository.findSelectedByRecommendationJobId(789L))
			.willReturn(Optional.empty());
		given(recommendationCandidateItemRepository.findAllByRecommendationCandidateRecommendationCandidateIdOrderByDayNumberAscVisitOrderAsc(1001L))
			.willReturn(List.of(item));
		given(albumRepository.save(org.mockito.ArgumentMatchers.any(Album.class))).willReturn(album);

		SelectRecommendationCandidateResponse response = recommendationService.selectCandidate(
			1L,
			789L,
			new SelectRecommendationCandidateRequest(1001L)
		);

		assertThat(candidate.isSelected()).isTrue();
		assertThat(course.getStatus()).isEqualTo(CourseStatus.UPCOMING);
		assertThat(response.albumId()).isEqualTo(3001L);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<CourseScheduleItem>> captor = ArgumentCaptor.forClass(List.class);
		verify(courseScheduleItemRepository).saveAll(captor.capture());
		assertThat(captor.getValue()).hasSize(1);
		assertThat(captor.getValue().get(0).getPlace().getPlaceId()).isEqualTo(501L);
	}

	@Test
	void selectCandidate_success_whenSameCandidateAlreadySelected() {
		Course course = courseWithId(456L);
		RecommendationJob job = completedJobWithId(789L, course);
		RecommendationCandidate candidate = candidateWithId(1001L, job, 1);
		candidate.select();
		Album album = Album.create(course);
		ReflectionTestUtils.setField(album, "albumId", 3001L);

		given(recommendationJobRepository.findWithCourseByRecommendationJobId(789L)).willReturn(Optional.of(job));
		given(recommendationCandidateRepository.findByRecommendationCandidateIdAndRecommendationJobRecommendationJobId(1001L, 789L))
			.willReturn(Optional.of(candidate));
		given(recommendationCandidateRepository.findSelectedByRecommendationJobId(789L))
			.willReturn(Optional.of(candidate));
		given(albumRepository.findFirstByCourseCourseIdOrderByAlbumIdAsc(456L)).willReturn(Optional.of(album));

		SelectRecommendationCandidateResponse response = recommendationService.selectCandidate(
			1L,
			789L,
			new SelectRecommendationCandidateRequest(1001L)
		);

		assertThat(response.selectedCandidateId()).isEqualTo(1001L);
		assertThat(response.albumId()).isEqualTo(3001L);
	}

	@Test
	void selectCandidate_fail_whenDifferentCandidateAlreadySelected() {
		Course course = courseWithId(456L);
		RecommendationJob job = completedJobWithId(789L, course);
		RecommendationCandidate candidate = candidateWithId(1001L, job, 1);
		RecommendationCandidate selected = candidateWithId(1002L, job, 2);
		selected.select();

		given(recommendationJobRepository.findWithCourseByRecommendationJobId(789L)).willReturn(Optional.of(job));
		given(recommendationCandidateRepository.findByRecommendationCandidateIdAndRecommendationJobRecommendationJobId(1001L, 789L))
			.willReturn(Optional.of(candidate));
		given(recommendationCandidateRepository.findSelectedByRecommendationJobId(789L))
			.willReturn(Optional.of(selected));

		assertThatThrownBy(() -> recommendationService.selectCandidate(
			1L,
			789L,
			new SelectRecommendationCandidateRequest(1001L)
		))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(RecommendationErrorCode.RECOMMENDATION_CANDIDATE_ALREADY_SELECTED);
	}

	private Course courseWithId(Long courseId) {
		UserFixture fixture = new UserFixture();
		Course course = Course.create(
			fixture.creator(),
			Region.create("3", "1", "대전광역시 동구"),
			"대전 효도 여행",
			LocalDate.now().plusDays(1),
			LocalDate.now().plusDays(2)
		);
		ReflectionTestUtils.setField(course, "courseId", courseId);
		return course;
	}

	private RecommendationJob completedJobWithId(Long jobId, Course course) {
		RecommendationJob job = RecommendationJob.createPending(course);
		ReflectionTestUtils.setField(job, "recommendationJobId", jobId);
		job.start();
		job.complete();
		return job;
	}

	private RecommendationCandidate candidateWithId(Long candidateId, RecommendationJob job, int rank) {
		RecommendationCandidate candidate = RecommendationCandidate.create(
			job,
			rank,
			"부모님 편안함 우선 코스",
			"이동 부담을 줄인 코스입니다.",
			new BigDecimal("92.0"),
			12400,
			95
		);
		ReflectionTestUtils.setField(candidate, "recommendationCandidateId", candidateId);
		return candidate;
	}

	private RecommendationCandidateItem candidateItemWithId(
		Long itemId,
		RecommendationCandidate candidate,
		Place place,
		int dayNumber,
		int visitOrder
	) {
		RecommendationCandidateItem item = RecommendationCandidateItem.create(
			candidate,
			place,
			dayNumber,
			visitOrder,
			LocalTime.of(10, 0),
			LocalTime.of(12, 0),
			TransportMode.CAR,
			24,
			8400
		);
		ReflectionTestUtils.setField(item, "candidateItemId", itemId);
		return item;
	}

	private Place placeWithId(Long placeId) {
		return placeWithId(placeId, "126508", "12");
	}

	private Place placeWithId(Long placeId, String contentId, String contentTypeId) {
		Place place = Place.create(
			contentId,
			contentTypeId,
			"TOUR_API",
			"NATURE",
			Region.create("3", "1", "대전광역시 동구"),
			"한밭수목원",
			"대전광역시 서구 둔산대로 169",
			new BigDecimal("36.366"),
			new BigDecimal("127.388"),
			"https://example.com/place.jpg"
		);
		ReflectionTestUtils.setField(place, "placeId", placeId);
		return place;
	}

	private AiRecommendationGenerateResponse aiResponseWithPlace(Long generationId, Long courseId, Long placeId) {
		return singleCandidateResponse(generationId, courseId, itemWithPlaceId(placeId));
	}

	private AiRecommendationGenerateResponse singleCandidateResponse(
		Long generationId,
		Long courseId,
		AiRecommendationGenerateResponse.ItemResponse... items
	) {
		return new AiRecommendationGenerateResponse(
			generationId,
			courseId,
			List.of(new AiRecommendationGenerateResponse.CandidateResponse(
				1,
				"부모님 편안함 우선 코스",
				"이동 부담을 줄인 코스입니다.",
				"필수 편의시설 조건을 우선 반영했습니다.",
				new BigDecimal("92.0"),
				150,
				List.of(new AiRecommendationGenerateResponse.DayResponse(1, List.of(items))),
				List.of()
			))
		);
	}

	private AiRecommendationGenerateResponse.ItemResponse itemWithPlaceId(Long placeId) {
		return itemWithPlaceId(placeId, null);
	}

	private AiRecommendationGenerateResponse.ItemResponse itemWithPlaceId(Long placeId, String imageUrl) {
		return new AiRecommendationGenerateResponse.ItemResponse(
			1, "한밭수목원", "TOUR", "MOCK", placeId,
			null, "126508", "12", "대전광역시 서구 둔산대로 169", "3", "1",
			new BigDecimal("36.366"), new BigDecimal("127.388"), imageUrl,
			70, List.of("휴식 공간 필요")
		);
	}

	private AiRecommendationGenerateResponse.ItemResponse newPlaceItem(String contentId, String contentTypeId, String name) {
		return new AiRecommendationGenerateResponse.ItemResponse(
			1, name, "TOUR", "TOUR_API", null,
			contentId, contentId, contentTypeId, "대전광역시 대덕구 신상로 65", "3", "1",
			new BigDecimal("36.362"), new BigDecimal("127.459"), null,
			60, List.of()
		);
	}

	private record UserFixture(com.withgahyo.domain.user.entity.User creator) {
		private UserFixture() {
			this(com.withgahyo.domain.user.entity.User.create("KAKAO", "creator", "작성자", null));
			ReflectionTestUtils.setField(creator, "userId", 1L);
		}
	}
}
