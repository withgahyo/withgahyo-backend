package com.withgahyo.domain.recommendation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.withgahyo.domain.course.entity.CourseStatus;
import com.withgahyo.domain.recommendation.dto.RecommendationCandidateDetailResponse;
import com.withgahyo.domain.recommendation.dto.RecommendationCandidatesResponse;
import com.withgahyo.domain.recommendation.dto.RecommendationStatusResponse;
import com.withgahyo.domain.recommendation.dto.SelectRecommendationCandidateRequest;
import com.withgahyo.domain.recommendation.dto.SelectRecommendationCandidateResponse;
import com.withgahyo.domain.recommendation.dto.StartRecommendationRequest;
import com.withgahyo.domain.recommendation.dto.StartRecommendationResponse;
import com.withgahyo.domain.recommendation.entity.RecommendationJobStatus;
import com.withgahyo.domain.recommendation.service.RecommendationService;
import com.withgahyo.global.security.AuthenticatedUser;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class RecommendationControllerTest {

	@Mock
	private RecommendationService recommendationService;

	private RecommendationController recommendationController;

	@BeforeEach
	void setUp() {
		recommendationController = new RecommendationController(recommendationService);
	}

	@Test
	void startGeneration_returnsAcceptedResponse() {
		StartRecommendationRequest request = new StartRecommendationRequest(null, List.of(), null);
		StartRecommendationResponse serviceResponse = new StartRecommendationResponse(
			789L,
			456L,
			RecommendationJobStatus.PENDING,
			LocalDateTime.now()
		);

		given(recommendationService.startGeneration(1L, 456L, request)).willReturn(serviceResponse);

		ResponseEntity<?> response = recommendationController.startGeneration(new AuthenticatedUser(1L), 456L, request);

		assertThat(response.getStatusCode().value()).isEqualTo(202);
		verify(recommendationService).startGeneration(1L, 456L, request);
	}

	@Test
	void getGenerationStatus_returnsStatusResponse() {
		RecommendationStatusResponse serviceResponse = new RecommendationStatusResponse(
			789L,
			456L,
			RecommendationJobStatus.RUNNING,
			null,
			null,
			null,
			null,
			null,
			LocalDateTime.now(),
			null
		);

		given(recommendationService.getGenerationStatus(1L, 789L)).willReturn(serviceResponse);

		var response = recommendationController.getGenerationStatus(new AuthenticatedUser(1L), 789L);

		assertThat(response.data()).isEqualTo(serviceResponse);
		verify(recommendationService).getGenerationStatus(1L, 789L);
	}

	@Test
	void getCandidates_returnsCandidatesResponse() {
		RecommendationCandidatesResponse serviceResponse = new RecommendationCandidatesResponse(789L, 456L, null, List.of());

		given(recommendationService.getCandidates(1L, 789L)).willReturn(serviceResponse);

		var response = recommendationController.getCandidates(new AuthenticatedUser(1L), 789L);

		assertThat(response.data()).isEqualTo(serviceResponse);
		verify(recommendationService).getCandidates(1L, 789L);
	}

	@Test
	void getCandidateDetail_returnsDetailResponse() {
		RecommendationCandidateDetailResponse serviceResponse = new RecommendationCandidateDetailResponse(
			789L,
			1001L,
			"부모님 편안함 우선 코스",
			"이동 부담을 줄인 코스입니다.",
			null,
			List.of(),
			List.of(),
			List.of(),
			List.of()
		);

		given(recommendationService.getCandidateDetail(1L, 789L, 1001L)).willReturn(serviceResponse);

		var response = recommendationController.getCandidateDetail(new AuthenticatedUser(1L), 789L, 1001L);

		assertThat(response.data()).isEqualTo(serviceResponse);
		verify(recommendationService).getCandidateDetail(1L, 789L, 1001L);
	}

	@Test
	void selectCandidate_returnsSelectionResponse() {
		SelectRecommendationCandidateRequest request = new SelectRecommendationCandidateRequest(1001L);
		SelectRecommendationCandidateResponse serviceResponse = new SelectRecommendationCandidateResponse(
			456L,
			789L,
			1001L,
			CourseStatus.UPCOMING,
			3001L,
			LocalDateTime.now()
		);

		given(recommendationService.selectCandidate(1L, 789L, request)).willReturn(serviceResponse);

		var response = recommendationController.selectCandidate(new AuthenticatedUser(1L), 789L, request);

		assertThat(response.data()).isEqualTo(serviceResponse);
		verify(recommendationService).selectCandidate(1L, 789L, request);
	}
}
