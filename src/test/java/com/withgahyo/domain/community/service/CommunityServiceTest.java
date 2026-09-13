package com.withgahyo.domain.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.withgahyo.domain.community.dto.CreateCommunityCommentRequest;
import com.withgahyo.domain.community.dto.ReportCommunityPostRequest;
import com.withgahyo.domain.community.entity.CommunityUserBlock;
import com.withgahyo.domain.community.entity.CommunityComment;
import com.withgahyo.domain.community.entity.CommunityPost;
import com.withgahyo.domain.community.entity.CommunityPostReport;
import com.withgahyo.domain.community.exception.CommunityErrorCode;
import com.withgahyo.domain.community.repository.CommunityCommentRepository;
import com.withgahyo.domain.community.repository.CommunityPostRepository;
import com.withgahyo.domain.community.repository.CommunityPostReportRepository;
import com.withgahyo.domain.community.repository.CommunityUserBlockRepository;
import com.withgahyo.domain.user.entity.User;
import com.withgahyo.domain.user.repository.UserRepository;
import com.withgahyo.global.exception.BusinessException;
import com.withgahyo.global.exception.code.SecurityErrorCode;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

	@Mock
	private UserRepository userRepository;

	@Mock
	private CommunityPostReportRepository communityPostReportRepository;

	@Mock
	private CommunityUserBlockRepository communityUserBlockRepository;

	private CommunityService communityService;

	@BeforeEach
	void setUp() {
		communityService = new CommunityService(
			communityPostRepository,
			communityCommentRepository,
			userRepository,
			communityPostReportRepository,
			communityUserBlockRepository
		);
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

	@Test
	void getPostDetail_returnsPostWithCommentCount() {
		User author = userWithId(User.create("KAKAO", "author", "작성자", null), 2L);
		CommunityPost post = postWithId(
			CommunityPost.create(author, "REVIEW", "추천 여행 후기", "가족 여행에 좋았습니다."),
			11L,
			LocalDateTime.of(2026, 9, 13, 11, 0)
		);
		post.increaseLikeCount();

		given(communityPostRepository.findActiveById(11L)).willReturn(Optional.of(post));
		given(communityCommentRepository.countActiveCommentsByPostId(11L)).willReturn(4L);

		var response = communityService.getPostDetail(1L, 11L);

		assertThat(response.postId()).isEqualTo(11L);
		assertThat(response.authorId()).isEqualTo(2L);
		assertThat(response.commentCount()).isEqualTo(4);
		assertThat(response.likeCount()).isEqualTo(1);
	}

	@Test
	void getPostDetail_fail_whenPostNotFound() {
		given(communityPostRepository.findActiveById(404L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> communityService.getPostDetail(1L, 404L))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(CommunityErrorCode.POST_NOT_FOUND)
			);
	}

	@Test
	void getPostShareUrl_returnsFrontendShareUrl() {
		User author = userWithId(User.create("KAKAO", "author", "작성자", null), 2L);
		CommunityPost post = postWithId(
			CommunityPost.create(author, "REVIEW", "추천 여행 후기", "가족 여행에 좋았습니다."),
			11L,
			LocalDateTime.of(2026, 9, 13, 11, 0)
		);

		given(communityPostRepository.findActiveById(11L)).willReturn(Optional.of(post));

		var response = communityService.getPostShareUrl(1L, 11L);

		assertThat(response.postId()).isEqualTo(11L);
		assertThat(response.shareUrl()).isEqualTo("https://api.gatigahyo.com/community/posts/11");
	}

	@Test
	void getComments_returnsPage() {
		User author = userWithId(User.create("KAKAO", "author", "작성자", null), 2L);
		CommunityPost post = postWithId(
			CommunityPost.create(author, "REVIEW", "추천 여행 후기", "가족 여행에 좋았습니다."),
			11L,
			LocalDateTime.of(2026, 9, 13, 11, 0)
		);
		CommunityComment first = commentWithId(
			CommunityComment.create(post, author, "좋은 정보 감사합니다."),
			30L,
			LocalDateTime.of(2026, 9, 13, 12, 0)
		);
		CommunityComment extra = commentWithId(
			CommunityComment.create(post, author, "다음 페이지 댓글입니다."),
			29L,
			LocalDateTime.of(2026, 9, 13, 11, 30)
		);

		given(communityPostRepository.findActiveById(11L)).willReturn(Optional.of(post));
		given(communityCommentRepository.findActiveCommentsByPostId(11L, null, 2)).willReturn(List.of(first, extra));

		var response = communityService.getComments(1L, 11L, null, 1);

		assertThat(response.comments()).hasSize(1);
		assertThat(response.comments().get(0).commentId()).isEqualTo(30L);
		assertThat(response.hasNext()).isTrue();
		assertThat(response.nextCursor()).isEqualTo("30");
	}

	@Test
	void createComment_success_savesComment() {
		User requester = userWithId(User.create("KAKAO", "me", "나", null), 1L);
		User author = userWithId(User.create("KAKAO", "author", "작성자", null), 2L);
		CommunityPost post = postWithId(
			CommunityPost.create(author, "REVIEW", "추천 여행 후기", "가족 여행에 좋았습니다."),
			11L,
			LocalDateTime.of(2026, 9, 13, 11, 0)
		);
		CommunityComment savedComment = commentWithId(
			CommunityComment.create(post, requester, "좋은 정보 감사합니다."),
			30L,
			LocalDateTime.of(2026, 9, 13, 12, 0)
		);

		given(communityPostRepository.findActiveById(11L)).willReturn(Optional.of(post));
		given(userRepository.findById(1L)).willReturn(Optional.of(requester));
		given(communityCommentRepository.save(any(CommunityComment.class))).willReturn(savedComment);

		var response = communityService.createComment(
			1L,
			11L,
			new CreateCommunityCommentRequest(" 좋은 정보 감사합니다. ")
		);

		assertThat(response.commentId()).isEqualTo(30L);
		assertThat(response.content()).isEqualTo("좋은 정보 감사합니다.");
		verify(communityCommentRepository).save(any(CommunityComment.class));
	}

	@Test
	void createComment_fail_whenUserNotFound() {
		User author = userWithId(User.create("KAKAO", "author", "작성자", null), 2L);
		CommunityPost post = postWithId(
			CommunityPost.create(author, "REVIEW", "추천 여행 후기", "가족 여행에 좋았습니다."),
			11L,
			LocalDateTime.of(2026, 9, 13, 11, 0)
		);

		given(communityPostRepository.findActiveById(11L)).willReturn(Optional.of(post));
		given(userRepository.findById(1L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> communityService.createComment(1L, 11L, new CreateCommunityCommentRequest("댓글")))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(SecurityErrorCode.INVALID_TOKEN)
			);
	}

	@Test
	void reportPost_success_savesReport() {
		User reporter = userWithId(User.create("KAKAO", "me", "나", null), 1L);
		User author = userWithId(User.create("KAKAO", "author", "작성자", null), 2L);
		CommunityPost post = postWithId(
			CommunityPost.create(author, "REVIEW", "추천 여행 후기", "가족 여행에 좋았습니다."),
			11L,
			LocalDateTime.of(2026, 9, 13, 11, 0)
		);
		CommunityPostReport savedReport = reportWithId(
			CommunityPostReport.create(post, reporter, "SPAM", "광고 게시글입니다."),
			40L
		);

		given(communityPostRepository.findActiveById(11L)).willReturn(Optional.of(post));
		given(userRepository.findById(1L)).willReturn(Optional.of(reporter));
		given(communityPostReportRepository.save(any(CommunityPostReport.class))).willReturn(savedReport);

		var response = communityService.reportPost(
			1L,
			11L,
			new ReportCommunityPostRequest("SPAM", "광고 게시글입니다.")
		);

		assertThat(response.reportId()).isEqualTo(40L);
		assertThat(response.reason()).isEqualTo("SPAM");
		verify(communityPostReportRepository).save(any(CommunityPostReport.class));
	}

	@Test
	void blockUser_success_savesBlock() {
		User requester = userWithId(User.create("KAKAO", "me", "나", null), 1L);
		User blockedUser = userWithId(User.create("KAKAO", "blocked", "차단대상", null), 2L);
		CommunityUserBlock savedBlock = blockWithId(CommunityUserBlock.create(requester, blockedUser), 50L);

		given(userRepository.findById(1L)).willReturn(Optional.of(requester));
		given(userRepository.findById(2L)).willReturn(Optional.of(blockedUser));
		given(communityUserBlockRepository.save(any(CommunityUserBlock.class))).willReturn(savedBlock);

		var response = communityService.blockUser(1L, 2L);

		assertThat(response.blockId()).isEqualTo(50L);
		assertThat(response.blockedUserId()).isEqualTo(2L);
		verify(communityUserBlockRepository).save(any(CommunityUserBlock.class));
	}

	@Test
	void blockUser_fail_whenBlockingSelf() {
		assertThatThrownBy(() -> communityService.blockUser(1L, 1L))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(CommunityErrorCode.SELF_BLOCK_NOT_ALLOWED)
			);
	}

	private User userWithId(User user, Long userId) {
		return setField(user, "userId", userId);
	}

	private CommunityPost postWithId(CommunityPost post, Long postId, LocalDateTime createdAt) {
		setField(post, "postId", postId);
		setField(post, "createdAt", createdAt);
		return post;
	}

	private CommunityComment commentWithId(CommunityComment comment, Long commentId, LocalDateTime createdAt) {
		setField(comment, "commentId", commentId);
		setField(comment, "createdAt", createdAt);
		return comment;
	}

	private CommunityPostReport reportWithId(CommunityPostReport report, Long reportId) {
		setField(report, "reportId", reportId);
		return report;
	}

	private CommunityUserBlock blockWithId(CommunityUserBlock block, Long blockId) {
		setField(block, "blockId", blockId);
		return block;
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
