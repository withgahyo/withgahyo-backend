package com.withgahyo.domain.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.withgahyo.domain.community.entity.CommunityPost;
import com.withgahyo.domain.community.repository.CommunityCommentRepository;
import com.withgahyo.domain.community.repository.CommunityPostRepository;
import com.withgahyo.domain.user.entity.User;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommunityServiceTest {

	@Mock
	private CommunityPostRepository communityPostRepository;

	@Mock
	private CommunityCommentRepository communityCommentRepository;

	private CommunityService communityService;

	@BeforeEach
	void setUp() {
		communityService = new CommunityService(communityPostRepository, communityCommentRepository);
	}

	@Test
	void getPosts_returnsPageWithCommentCounts() {
		User author = userWithId(User.create("KAKAO", "author", "작성자", null), 2L);
		CommunityPost first = postWithId(
			CommunityPost.create(author, "TRAVEL_TIP", "대전 가족 여행 팁", "휠체어 동선이 편한 코스입니다."),
			10L,
			LocalDateTime.of(2026, 9, 13, 10, 0)
		);
		CommunityPost second = postWithId(
			CommunityPost.create(author, "REVIEW", "대전 여행 후기", "부모님과 함께 다녀왔어요."),
			9L,
			LocalDateTime.of(2026, 9, 13, 9, 0)
		);
		CommunityPost extra = postWithId(
			CommunityPost.create(author, "QUESTION", "추가 게시글", "다음 페이지 확인용입니다."),
			8L,
			LocalDateTime.of(2026, 9, 13, 8, 0)
		);

		given(communityPostRepository.searchActivePosts("대전", "TRAVEL_TIP", "latest", 20L, 3))
			.willReturn(List.of(first, second, extra));
		given(communityCommentRepository.countActiveCommentsByPostIds(List.of(10L, 9L)))
			.willReturn(Map.of(10L, 3L, 9L, 1L));

		var response = communityService.getPosts(1L, "대전", "TRAVEL_TIP", "latest", 20L, 2);

		assertThat(response.posts()).hasSize(2);
		assertThat(response.posts().get(0).postId()).isEqualTo(10L);
		assertThat(response.posts().get(0).commentCount()).isEqualTo(3);
		assertThat(response.posts().get(1).postId()).isEqualTo(9L);
		assertThat(response.hasNext()).isTrue();
		assertThat(response.nextCursor()).isEqualTo("9");
	}

	@Test
	void getRecommendedPosts_returnsPopularPostsWithoutNextCursor() {
		User author = userWithId(User.create("KAKAO", "author", "작성자", null), 2L);
		CommunityPost post = postWithId(
			CommunityPost.create(author, "REVIEW", "추천 여행 후기", "가족 여행에 좋았습니다."),
			11L,
			LocalDateTime.of(2026, 9, 13, 11, 0)
		);
		post.increaseLikeCount();

		given(communityPostRepository.findRecommendedActivePosts(5)).willReturn(List.of(post));
		given(communityCommentRepository.countActiveCommentsByPostIds(List.of(11L))).willReturn(Map.of(11L, 4L));

		var response = communityService.getRecommendedPosts(1L, 5);

		assertThat(response.posts()).hasSize(1);
		assertThat(response.posts().get(0).postId()).isEqualTo(11L);
		assertThat(response.posts().get(0).likeCount()).isEqualTo(1);
		assertThat(response.posts().get(0).commentCount()).isEqualTo(4);
		assertThat(response.hasNext()).isFalse();
		assertThat(response.nextCursor()).isNull();
	}

	private User userWithId(User user, Long userId) {
		return setField(user, "userId", userId);
	}

	private CommunityPost postWithId(CommunityPost post, Long postId, LocalDateTime createdAt) {
		setField(post, "postId", postId);
		setField(post, "createdAt", createdAt);
		return post;
	}

	private <T> T setField(T target, String fieldName, Object value) {
		try {
			Field field = target.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(target, value);
			return target;
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
