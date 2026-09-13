package com.withgahyo.domain.review.controller;

import com.withgahyo.domain.review.dto.CreateReviewRequest;
import com.withgahyo.domain.review.dto.PendingReviewListResponse;
import com.withgahyo.domain.review.dto.ReviewFormResponse;
import com.withgahyo.domain.review.dto.ReviewResponse;
import com.withgahyo.domain.review.service.ReviewService;
import com.withgahyo.global.exception.BusinessException;
import com.withgahyo.global.exception.code.SecurityErrorCode;
import com.withgahyo.global.response.ApiResponse;
import com.withgahyo.global.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ReviewController {

	private final ReviewService reviewService;

	public ReviewController(ReviewService reviewService) {
		this.reviewService = reviewService;
	}

	@GetMapping("/users/me/reviews/pending")
	public ApiResponse<PendingReviewListResponse> getPendingReviews(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser
	) {
		return ApiResponse.success(reviewService.getPendingReviews(requireUserId(authenticatedUser)));
	}

	@PostMapping("/courses/{courseId}/reviews")
	public ApiResponse<ReviewResponse> createReview(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable Long courseId,
		@Valid @RequestBody CreateReviewRequest request
	) {
		return ApiResponse.success(reviewService.createReview(requireUserId(authenticatedUser), courseId, request));
	}

	@GetMapping("/courses/{courseId}/review-form")
	public ApiResponse<ReviewFormResponse> getReviewForm(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable Long courseId
	) {
		return ApiResponse.success(reviewService.getReviewForm(requireUserId(authenticatedUser), courseId));
	}

	@GetMapping("/courses/{courseId}/reviews/me")
	public ApiResponse<ReviewResponse> getMyReview(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable Long courseId
	) {
		return ApiResponse.success(reviewService.getMyReview(requireUserId(authenticatedUser), courseId));
	}

	private Long requireUserId(AuthenticatedUser authenticatedUser) {
		if (authenticatedUser == null) {
			throw new BusinessException(SecurityErrorCode.UNAUTHORIZED);
		}
		return authenticatedUser.userId();
	}
}
