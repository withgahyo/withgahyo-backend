package com.withgahyo.domain.community.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.withgahyo.domain.community.dto.CommunityPostListResponse;
import com.withgahyo.domain.community.dto.CommunityPostDetailResponse;
import com.withgahyo.domain.community.dto.CommunityPostShareUrlResponse;
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
	void getPosts_returnsFilteredPostList() {
		CommunityPostListResponse serviceResponse = new CommunityPostListResponse(
			List.of(new CommunityPostListResponse.PostSummaryResponse(
				10L,
				1L,
				"지우",
				"TRAVEL_TIP",
				"대전 가족 여행 팁",
				"휠체어 동선이 편한 코스입니다.",
				3,
				2,
				LocalDateTime.of(2026, 9, 13, 10, 0)
			)),
			true,
			"10"
		);

		given(communityService.getPosts(1L, "대전", "TRAVEL_TIP", "latest", 20L, 10))
			.willReturn(serviceResponse);

		var response = communityController.getPosts(
			new AuthenticatedUser(1L),
			"대전",
			"TRAVEL_TIP",
			"latest",
			20L,
			10
		);

		assertThat(response.data()).isEqualTo(serviceResponse);
		verify(communityService).getPosts(1L, "대전", "TRAVEL_TIP", "latest", 20L, 10);
	}

	@Test
	void getRecommendedPosts_returnsRecommendedPostList() {
		CommunityPostListResponse serviceResponse = new CommunityPostListResponse(
			List.of(new CommunityPostListResponse.PostSummaryResponse(
				11L,
				2L,
				"가효",
				"REVIEW",
				"부모님과 다녀온 추천 코스",
				"주차장과 엘리베이터가 가까웠어요.",
				8,
				4,
				LocalDateTime.of(2026, 9, 13, 11, 0)
			)),
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
		CommunityPostDetailResponse serviceResponse = new CommunityPostDetailResponse(
			10L,
			2L,
			"가효",
			"REVIEW",
			"부모님과 다녀온 여행",
			"주차장과 엘리베이터가 가까웠어요.",
			4,
			8,
			LocalDateTime.of(2026, 9, 13, 11, 0)
		);

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
}
