package com.withgahyo.domain.community.controller;

import com.withgahyo.domain.community.dto.CommunityPostListResponse;
import com.withgahyo.domain.community.service.CommunityService;
import com.withgahyo.global.exception.BusinessException;
import com.withgahyo.global.exception.code.SecurityErrorCode;
import com.withgahyo.global.response.ApiResponse;
import com.withgahyo.global.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/community")
public class CommunityController {

	private final CommunityService communityService;

	public CommunityController(CommunityService communityService) {
		this.communityService = communityService;
	}

	@GetMapping("/posts")
	public ApiResponse<CommunityPostListResponse> getPosts(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@RequestParam(required = false) String keyword,
		@RequestParam(required = false) String category,
		@RequestParam(defaultValue = "latest") String sort,
		@RequestParam(required = false) Long cursor,
		@RequestParam(defaultValue = "20") Integer size
	) {
		return ApiResponse.success(
			communityService.getPosts(requireUserId(authenticatedUser), keyword, category, sort, cursor, size)
		);
	}

	@GetMapping("/posts/recommendations")
	public ApiResponse<CommunityPostListResponse> getRecommendedPosts(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@RequestParam(defaultValue = "5") Integer size
	) {
		return ApiResponse.success(communityService.getRecommendedPosts(requireUserId(authenticatedUser), size));
	}

	private Long requireUserId(AuthenticatedUser authenticatedUser) {
		if (authenticatedUser == null) {
			throw new BusinessException(SecurityErrorCode.UNAUTHORIZED);
		}
		return authenticatedUser.userId();
	}
}
