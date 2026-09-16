package com.withgahyo.domain.community.controller;

import com.withgahyo.domain.community.dto.BlockCommunityUserResponse;
import com.withgahyo.domain.community.dto.CommunityCommentListResponse;
import com.withgahyo.domain.community.dto.CommunityPostDetailResponse;
import com.withgahyo.domain.community.dto.CommunityPostLikeResponse;
import com.withgahyo.domain.community.dto.CommunityPostListResponse;
import com.withgahyo.domain.community.dto.CommunityPostShareUrlResponse;
import com.withgahyo.domain.community.dto.CreateCommunityCommentRequest;
import com.withgahyo.domain.community.dto.CreateCommunityCommentResponse;
import com.withgahyo.domain.community.dto.CreateCommunityPostRequest;
import com.withgahyo.domain.community.dto.ReportCommunityPostRequest;
import com.withgahyo.domain.community.dto.ReportCommunityPostResponse;
import com.withgahyo.domain.community.service.CommunityService;
import com.withgahyo.global.exception.BusinessException;
import com.withgahyo.global.exception.code.SecurityErrorCode;
import com.withgahyo.global.response.ApiResponse;
import com.withgahyo.global.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

	@PostMapping("/posts")
	public ApiResponse<CommunityPostDetailResponse> createPost(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@Valid @RequestBody CreateCommunityPostRequest request
	) {
		return ApiResponse.success(communityService.createPost(requireUserId(authenticatedUser), request));
	}

	@DeleteMapping("/posts/{postId}")
	public ApiResponse<Void> deletePost(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable Long postId
	) {
		communityService.deletePost(requireUserId(authenticatedUser), postId);
		return ApiResponse.ok();
	}

	@GetMapping("/posts")
	public ApiResponse<CommunityPostListResponse> getPosts(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@RequestParam(required = false) String keyword,
		@RequestParam(required = false) String regionName,
		@RequestParam(required = false) String highlightType,
		@RequestParam(defaultValue = "latest") String sort,
		@RequestParam(required = false) Long cursor,
		@RequestParam(defaultValue = "20") Integer size
	) {
		return ApiResponse.success(
			communityService.getPosts(
				requireUserId(authenticatedUser),
				keyword,
				regionName,
				highlightType,
				sort,
				cursor,
				size
			)
		);
	}

	@GetMapping("/posts/recommendations")
	public ApiResponse<CommunityPostListResponse> getRecommendedPosts(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@RequestParam(defaultValue = "5") Integer size
	) {
		return ApiResponse.success(communityService.getRecommendedPosts(requireUserId(authenticatedUser), size));
	}

	@GetMapping("/posts/{postId}")
	public ApiResponse<CommunityPostDetailResponse> getPostDetail(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable Long postId
	) {
		return ApiResponse.success(communityService.getPostDetail(requireUserId(authenticatedUser), postId));
	}

	@GetMapping("/posts/{postId}/share-url")
	public ApiResponse<CommunityPostShareUrlResponse> getPostShareUrl(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable Long postId
	) {
		return ApiResponse.success(communityService.getPostShareUrl(requireUserId(authenticatedUser), postId));
	}

	@PostMapping("/posts/{postId}/likes")
	public ApiResponse<CommunityPostLikeResponse> likePost(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable Long postId
	) {
		return ApiResponse.success(communityService.likePost(requireUserId(authenticatedUser), postId));
	}

	@DeleteMapping("/posts/{postId}/likes")
	public ApiResponse<CommunityPostLikeResponse> unlikePost(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable Long postId
	) {
		return ApiResponse.success(communityService.unlikePost(requireUserId(authenticatedUser), postId));
	}

	@GetMapping("/posts/{postId}/comments")
	public ApiResponse<CommunityCommentListResponse> getComments(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable Long postId,
		@RequestParam(required = false) Long cursor,
		@RequestParam(defaultValue = "20") Integer size
	) {
		return ApiResponse.success(communityService.getComments(requireUserId(authenticatedUser), postId, cursor, size));
	}

	@PostMapping("/posts/{postId}/comments")
	public ApiResponse<CreateCommunityCommentResponse> createComment(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable Long postId,
		@Valid @RequestBody CreateCommunityCommentRequest request
	) {
		return ApiResponse.success(communityService.createComment(requireUserId(authenticatedUser), postId, request));
	}

	@PostMapping("/posts/{postId}/reports")
	public ApiResponse<ReportCommunityPostResponse> reportPost(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable Long postId,
		@Valid @RequestBody ReportCommunityPostRequest request
	) {
		return ApiResponse.success(communityService.reportPost(requireUserId(authenticatedUser), postId, request));
	}

	@PostMapping("/users/{userId}/blocks")
	public ApiResponse<BlockCommunityUserResponse> blockUser(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable Long userId
	) {
		return ApiResponse.success(communityService.blockUser(requireUserId(authenticatedUser), userId));
	}

	private Long requireUserId(AuthenticatedUser authenticatedUser) {
		if (authenticatedUser == null) {
			throw new BusinessException(SecurityErrorCode.UNAUTHORIZED);
		}
		return authenticatedUser.userId();
	}
}
