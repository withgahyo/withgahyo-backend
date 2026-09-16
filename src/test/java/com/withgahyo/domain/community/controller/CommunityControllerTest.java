package com.withgahyo.domain.community.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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
import com.withgahyo.global.security.AuthenticatedUser;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommunityControllerTest {

	@Mock
	private CommunityService communityService;

	private CommunityController communityController;

	@BeforeEach
	void setUp() {
		communityController = new CommunityController(communityService);
	}

	@Test
	void createPost_returnsSharedPostDetail() {
		CreateCommunityPostRequest request = new CreateCommunityPostRequest(100L);
		CommunityPostDetailResponse serviceResponse = samplePostDetail();

		given(communityService.createPost(1L, request)).willReturn(serviceResponse);

		var response = communityController.createPost(new AuthenticatedUser(1L), request);

		assertThat(response.data()).isEqualTo(serviceResponse);
		verify(communityService).createPost(1L, request);
	}

	@Test
	void deletePost_deletesOwnPost() {
		var response = communityController.deletePost(new AuthenticatedUser(1L), 10L);

		assertThat(response.success()).isTrue();
		verify(communityService).deletePost(1L, 10L);
	}

	@Test
	void getPosts_returnsFilteredPostList() {
		CommunityPostListResponse serviceResponse = new CommunityPostListResponse(
			List.of(samplePostSummary()),
			true,
			"10"
		);

		given(communityService.getPosts(1L, "대전", "부산", "맛집", "latest", 20L, 10))
			.willReturn(serviceResponse);

		var response = communityController.getPosts(
			new AuthenticatedUser(1L),
			"대전",
			"부산",
			"맛집",
			"latest",
			20L,
			10
		);

		assertThat(response.data()).isEqualTo(serviceResponse);
		verify(communityService).getPosts(1L, "대전", "부산", "맛집", "latest", 20L, 10);
	}

	@Test
	void getRecommendedPosts_returnsRecommendedPostList() {
		CommunityPostListResponse serviceResponse = new CommunityPostListResponse(
			List.of(samplePostSummary()),
			false,
			null
		);

		given(communityService.getRecommendedPosts(1L, 5)).willReturn(serviceResponse);

		var response = communityController.getRecommendedPosts(new AuthenticatedUser(1L), 5);

		assertThat(response.data()).isEqualTo(serviceResponse);
		verify(communityService).getRecommendedPosts(1L, 5);
	}

	@Test
	void getPostDetail_returnsPostDetail() {
		CommunityPostDetailResponse serviceResponse = samplePostDetail();

		given(communityService.getPostDetail(1L, 10L)).willReturn(serviceResponse);

		var response = communityController.getPostDetail(new AuthenticatedUser(1L), 10L);

		assertThat(response.data()).isEqualTo(serviceResponse);
		verify(communityService).getPostDetail(1L, 10L);
	}

	@Test
	void getPostShareUrl_returnsShareUrl() {
		CommunityPostShareUrlResponse serviceResponse = new CommunityPostShareUrlResponse(
			10L,
			"https://api.gatigahyo.com/community/posts/10"
		);

		given(communityService.getPostShareUrl(1L, 10L)).willReturn(serviceResponse);

		var response = communityController.getPostShareUrl(new AuthenticatedUser(1L), 10L);

		assertThat(response.data()).isEqualTo(serviceResponse);
		verify(communityService).getPostShareUrl(1L, 10L);
	}

	@Test
	void likePost_returnsLikeResponse() {
		CommunityPostLikeResponse serviceResponse = CommunityPostLikeResponse.of(10L, true, 5);

		given(communityService.likePost(1L, 10L)).willReturn(serviceResponse);

		var response = communityController.likePost(new AuthenticatedUser(1L), 10L);

		assertThat(response.data()).isEqualTo(serviceResponse);
		verify(communityService).likePost(1L, 10L);
	}

	@Test
	void unlikePost_returnsLikeResponse() {
		CommunityPostLikeResponse serviceResponse = CommunityPostLikeResponse.of(10L, false, 4);

		given(communityService.unlikePost(1L, 10L)).willReturn(serviceResponse);

		var response = communityController.unlikePost(new AuthenticatedUser(1L), 10L);

		assertThat(response.data()).isEqualTo(serviceResponse);
		verify(communityService).unlikePost(1L, 10L);
	}

	@Test
	void getComments_returnsCommentList() {
		CommunityCommentListResponse serviceResponse = new CommunityCommentListResponse(
			List.of(new CommunityCommentListResponse.CommentResponse(
				30L,
				2L,
				"가효",
				"좋은 정보 감사합니다.",
				LocalDateTime.of(2026, 9, 13, 12, 0)
			)),
			false,
			null
		);

		given(communityService.getComments(1L, 10L, null, 20)).willReturn(serviceResponse);

		var response = communityController.getComments(new AuthenticatedUser(1L), 10L, null, 20);

		assertThat(response.data()).isEqualTo(serviceResponse);
		verify(communityService).getComments(1L, 10L, null, 20);
	}

	@Test
	void createComment_returnsCreatedComment() {
		CreateCommunityCommentRequest request = new CreateCommunityCommentRequest("좋은 정보 감사합니다.");
		CreateCommunityCommentResponse serviceResponse = new CreateCommunityCommentResponse(
			30L,
			10L,
			1L,
			"좋은 정보 감사합니다.",
			LocalDateTime.of(2026, 9, 13, 12, 0)
		);

		given(communityService.createComment(1L, 10L, request)).willReturn(serviceResponse);

		var response = communityController.createComment(new AuthenticatedUser(1L), 10L, request);

		assertThat(response.data()).isEqualTo(serviceResponse);
		verify(communityService).createComment(1L, 10L, request);
	}

	@Test
	void reportPost_returnsReportResponse() {
		ReportCommunityPostRequest request = new ReportCommunityPostRequest("SPAM", "광고 게시글입니다.");
		ReportCommunityPostResponse serviceResponse = new ReportCommunityPostResponse(40L, 10L, "SPAM");

		given(communityService.reportPost(1L, 10L, request)).willReturn(serviceResponse);

		var response = communityController.reportPost(new AuthenticatedUser(1L), 10L, request);

		assertThat(response.data()).isEqualTo(serviceResponse);
		verify(communityService).reportPost(1L, 10L, request);
	}

	@Test
	void blockUser_returnsBlockResponse() {
		BlockCommunityUserResponse serviceResponse = new BlockCommunityUserResponse(50L, 2L);

		given(communityService.blockUser(1L, 2L)).willReturn(serviceResponse);

		var response = communityController.blockUser(new AuthenticatedUser(1L), 2L);

		assertThat(response.data()).isEqualTo(serviceResponse);
		verify(communityService).blockUser(1L, 2L);
	}

	private CommunityPostListResponse.PostSummaryResponse samplePostSummary() {
		return new CommunityPostListResponse.PostSummaryResponse(
			10L,
			1L,
			"지우",
			"https://example.com/profile.png",
			5L,
			"대전 가족 여행",
			"대전",
			"https://example.com/course.png",
			(byte) 5,
			List.of("맛집", "편의시설"),
			"휠체어 동선이 편한 코스입니다.",
			3,
			2,
			false,
			LocalDateTime.of(2026, 9, 13, 10, 0)
		);
	}

	private CommunityPostDetailResponse samplePostDetail() {
		return new CommunityPostDetailResponse(
			10L,
			2L,
			"가효",
			"https://example.com/profile.png",
			5L,
			"부모님과 다녀온 여행",
			"대전",
			"https://example.com/course.png",
			(byte) 5,
			List.of("맛집", "편의시설"),
			"주차장과 엘리베이터가 가까웠어요.",
			4,
			8,
			false,
			LocalDateTime.of(2026, 9, 13, 11, 0)
		);
	}
}
