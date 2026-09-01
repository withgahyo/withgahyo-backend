package com.withgahyo.domain.recommendation.controller;

import com.withgahyo.domain.recommendation.dto.RecommendationCandidateDetailResponse;
import com.withgahyo.domain.recommendation.dto.RecommendationCandidatesResponse;
import com.withgahyo.domain.recommendation.dto.RecommendationStatusResponse;
import com.withgahyo.domain.recommendation.dto.SelectRecommendationCandidateRequest;
import com.withgahyo.domain.recommendation.dto.SelectRecommendationCandidateResponse;
import com.withgahyo.domain.recommendation.dto.StartRecommendationRequest;
import com.withgahyo.domain.recommendation.dto.StartRecommendationResponse;
import com.withgahyo.domain.recommendation.service.RecommendationService;
import com.withgahyo.global.exception.BusinessException;
import com.withgahyo.global.exception.code.SecurityErrorCode;
import com.withgahyo.global.response.ApiResponse;
import com.withgahyo.global.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class RecommendationController {

	private final RecommendationService recommendationService;

	public RecommendationController(RecommendationService recommendationService) {
		this.recommendationService = recommendationService;
	}

	@PostMapping("/courses/{courseId}/generations")
	public ResponseEntity<ApiResponse<StartRecommendationResponse>> startGeneration(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable Long courseId,
		@Valid @RequestBody(required = false) StartRecommendationRequest request
	) {
		StartRecommendationResponse response = recommendationService.startGeneration(
			requireUserId(authenticatedUser),
			courseId,
			request
		);
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(response));
	}

	@GetMapping("/course-generations/{generationId}")
	public ApiResponse<RecommendationStatusResponse> getGenerationStatus(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable Long generationId
	) {
		return ApiResponse.success(recommendationService.getGenerationStatus(requireUserId(authenticatedUser), generationId));
	}

	@GetMapping("/course-generations/{generationId}/candidates")
	public ApiResponse<RecommendationCandidatesResponse> getCandidates(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable Long generationId
	) {
		return ApiResponse.success(recommendationService.getCandidates(requireUserId(authenticatedUser), generationId));
	}

	@GetMapping("/course-generations/{generationId}/candidates/{candidateId}")
	public ApiResponse<RecommendationCandidateDetailResponse> getCandidateDetail(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable Long generationId,
		@PathVariable Long candidateId
	) {
		return ApiResponse.success(recommendationService.getCandidateDetail(
			requireUserId(authenticatedUser),
			generationId,
			candidateId
		));
	}

	@PostMapping("/course-generations/{generationId}/selection")
	public ApiResponse<SelectRecommendationCandidateResponse> selectCandidate(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable Long generationId,
		@Valid @RequestBody SelectRecommendationCandidateRequest request
	) {
		return ApiResponse.success(recommendationService.selectCandidate(
			requireUserId(authenticatedUser),
			generationId,
			request
		));
	}

	private Long requireUserId(AuthenticatedUser authenticatedUser) {
		if (authenticatedUser == null) {
			throw new BusinessException(SecurityErrorCode.UNAUTHORIZED);
		}
		return authenticatedUser.userId();
	}
}
