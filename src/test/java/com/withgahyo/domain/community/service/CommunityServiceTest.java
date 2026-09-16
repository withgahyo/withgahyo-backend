package com.withgahyo.domain.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.withgahyo.domain.community.dto.CreateCommunityCommentRequest;
import com.withgahyo.domain.community.dto.CreateCommunityPostRequest;
import com.withgahyo.domain.community.dto.ReportCommunityPostRequest;
import com.withgahyo.domain.community.entity.CommunityComment;
import com.withgahyo.domain.community.entity.CommunityPost;
import com.withgahyo.domain.community.entity.CommunityPostReport;
import com.withgahyo.domain.community.entity.CommunityUserBlock;
import com.withgahyo.domain.community.exception.CommunityErrorCode;
import com.withgahyo.domain.community.repository.CommunityCommentRepository;
import com.withgahyo.domain.community.repository.CommunityPostLikeRepository;
import com.withgahyo.domain.community.repository.CommunityPostRepository;
import com.withgahyo.domain.community.repository.CommunityPostReportRepository;
import com.withgahyo.domain.community.repository.CommunityUserBlockRepository;
import com.withgahyo.domain.course.entity.Course;
import com.withgahyo.domain.place.entity.Region;
import com.withgahyo.domain.review.entity.Review;
import com.withgahyo.domain.review.entity.ReviewHighlight;
import com.withgahyo.domain.review.exception.ReviewErrorCode;
import com.withgahyo.domain.review.repository.ReviewHighlightRepository;
import com.withgahyo.domain.review.repository.ReviewRepository;
import com.withgahyo.domain.user.entity.User;
import com.withgahyo.domain.user.repository.UserRepository;
import com.withgahyo.global.exception.BusinessException;
import com.withgahyo.global.exception.code.SecurityErrorCode;
import java.lang.reflect.Field;
import java.time.LocalDate;
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
	private CommunityPostLikeRepository communityPostLikeRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private CommunityPostReportRepository communityPostReportRepository;

	@Mock
	private CommunityUserBlockRepository communityUserBlockRepository;

	@Mock
	private ReviewRepository reviewRepository;

	@Mock
	private ReviewHighlightRepository reviewHighlightRepository;

	private CommunityService communityService;

	@BeforeEach
	void setUp() {
		communityService = new CommunityService(
			communityPostRepository,
			communityCommentRepository,
			communityPostLikeRepository,
			userRepository,
			communityPostReportRepository,
			communityUserBlockRepository,
			reviewRepository,
			reviewHighlightRepository
		);
	}

	@Test
	void getPosts_returnsPageWithCommentCounts() {
		User author = userWithId(User.create("KAKAO", "author", "작성자", null), 2L);
		Review firstReview = reviewWithId(sampleReview(author, "대전 가족 여행 팁"), 100L);
		Review secondReview = reviewWithId(sampleReview(author, "대전 여행 후기"), 99L);
		Review extraReview = reviewWithId(sampleReview(author, "추가 게시글"), 98L);
		CommunityPost first = postWithId(CommunityPost.create(firstReview), 10L, LocalDateTime.of(2026, 9, 13, 10, 0));
		CommunityPost second = postWithId(CommunityPost.create(secondReview), 9L, LocalDateTime.of(2026, 9, 13, 9, 0));
		CommunityPost extra = postWithId(CommunityPost.create(extraReview), 8L, LocalDateTime.of(2026, 9, 13, 8, 0));

		given(communityPostRepository.searchActivePosts("대전", null, null, "latest", 20L, 3))
			.willReturn(List.of(first, second, extra));
		given(communityCommentRepository.countActiveCommentsByPostIds(List.of(10L, 9L)))
			.willReturn(Map.of(10L, 3L, 9L, 1L));
		given(communityPostLikeRepository.findLikedPostIds(1L, List.of(10L, 9L))).willReturn(List.of());
		given(reviewHighlightRepository.findByReview_ReviewIdIn(List.of(100L, 99L))).willReturn(List.of());

		var response = communityService.getPosts(1L, "대전", null, null, "latest", 20L, 2);

		assertThat(response.posts()).hasSize(2);
		assertThat(response.posts().get(0).postId()).isEqualTo(10L);
		assertThat(response.posts().get(0).commentCount()).isEqualTo(3);
		assertThat(response.posts().get(1).postId()).isEqualTo(9L);
		assertThat(response.hasNext()).isTrue();
		assertThat(response.nextCursor()).isEqualTo("9");
	}

	@Test
	void getPosts_filtersByRegionName() {
		User author = userWithId(User.create("KAKAO", "author", "작성자", null), 2L);
		Review busanReview = reviewWithId(sampleReview(author, "부산", "부산 효도여행"), 100L);
		CommunityPost busanPost = postWithId(
			CommunityPost.create(busanReview),
			10L,
			LocalDateTime.of(2026, 9, 13, 10, 0)
		);

		given(communityPostRepository.searchActivePosts(null, "부산", null, "latest", null, 21))
			.willReturn(List.of(busanPost));
		given(communityCommentRepository.countActiveCommentsByPostIds(List.of(10L))).willReturn(Map.of());
		given(communityPostLikeRepository.findLikedPostIds(1L, List.of(10L))).willReturn(List.of());
		given(reviewHighlightRepository.findByReview_ReviewIdIn(List.of(100L))).willReturn(List.of());

		var response = communityService.getPosts(1L, null, "부산", null, "latest", null, 20);

		assertThat(response.posts()).hasSize(1);
		assertThat(response.posts().get(0).regionName()).isEqualTo("부산");
		verify(communityPostRepository).searchActivePosts(null, "부산", null, "latest", null, 21);
	}

	@Test
	void getRecommendedPosts_returnsPopularPostsWithoutNextCursor() {
		User author = userWithId(User.create("KAKAO", "author", "작성자", null), 2L);
		Review review = reviewWithId(sampleReview(author, "추천 여행 후기"), 100L);
		CommunityPost post = postWithId(CommunityPost.create(review), 11L, LocalDateTime.of(2026, 9, 13, 11, 0));
		post.increaseLikeCount();

		given(communityPostRepository.findRecommendedActivePosts(5)).willReturn(List.of(post));
		given(communityCommentRepository.countActiveCommentsByPostIds(List.of(11L))).willReturn(Map.of(11L, 4L));
		given(communityPostLikeRepository.findLikedPostIds(1L, List.of(11L))).willReturn(List.of());
		given(reviewHighlightRepository.findByReview_ReviewIdIn(List.of(100L))).willReturn(List.of());

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
		Review review = reviewWithId(sampleReview(author, "추천 여행 후기"), 100L);
		CommunityPost post = postWithId(CommunityPost.create(review), 11L, LocalDateTime.of(2026, 9, 13, 11, 0));
		post.increaseLikeCount();

		given(communityPostRepository.findActiveById(11L)).willReturn(Optional.of(post));
		given(communityPostLikeRepository.findLikedPostIds(1L, List.of(11L))).willReturn(List.of());
		given(reviewHighlightRepository.findByReview_ReviewId(100L)).willReturn(List.of());
		given(communityCommentRepository.countActiveCommentsByPostId(11L)).willReturn(4L);

		var response = communityService.getPostDetail(1L, 11L);

		assertThat(response.postId()).isEqualTo(11L);
		assertThat(response.authorId()).isEqualTo(2L);
		assertThat(response.commentCount()).isEqualTo(4);
		assertThat(response.likeCount()).isEqualTo(1);
		assertThat(response.likedByMe()).isFalse();
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
		Review review = reviewWithId(sampleReview(author, "추천 여행 후기"), 100L);
		CommunityPost post = postWithId(CommunityPost.create(review), 11L, LocalDateTime.of(2026, 9, 13, 11, 0));

		given(communityPostRepository.findActiveById(11L)).willReturn(Optional.of(post));

		var response = communityService.getPostShareUrl(1L, 11L);

		assertThat(response.postId()).isEqualTo(11L);
		assertThat(response.shareUrl()).isEqualTo("https://api.gatigahyo.com/community/posts/11");
	}

	@Test
	void createPost_success_sharesReview() {
		User author = userWithId(User.create("KAKAO", "author", "작성자", null), 1L);
		Review review = reviewWithId(sampleReview(author, "부모님과 다녀온 여행"), 100L);
		CommunityPost savedPost = postWithId(CommunityPost.create(review), 11L, LocalDateTime.of(2026, 9, 13, 11, 0));

		given(reviewRepository.findById(100L)).willReturn(Optional.of(review));
		given(communityPostRepository.existsByReview_ReviewIdAndDeletedAtIsNull(100L)).willReturn(false);
		given(communityPostRepository.save(any(CommunityPost.class))).willReturn(savedPost);
		given(reviewHighlightRepository.findByReview_ReviewId(100L)).willReturn(List.of());

		var response = communityService.createPost(1L, new CreateCommunityPostRequest(100L));

		assertThat(response.postId()).isEqualTo(11L);
		assertThat(response.authorId()).isEqualTo(1L);
		assertThat(response.likedByMe()).isFalse();
		verify(communityPostRepository).save(any(CommunityPost.class));
	}

	@Test
	void createPost_fail_whenReviewNotOwnedByRequester() {
		User author = userWithId(User.create("KAKAO", "author", "작성자", null), 2L);
		Review review = reviewWithId(sampleReview(author, "부모님과 다녀온 여행"), 100L);

		given(reviewRepository.findById(100L)).willReturn(Optional.of(review));

		assertThatThrownBy(() -> communityService.createPost(1L, new CreateCommunityPostRequest(100L)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(CommunityErrorCode.POST_ACCESS_DENIED)
			);
	}

	@Test
	void createPost_fail_whenReviewNotFound() {
		given(reviewRepository.findById(404L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> communityService.createPost(1L, new CreateCommunityPostRequest(404L)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ReviewErrorCode.REVIEW_NOT_FOUND)
			);
	}

	@Test
	void createPost_fail_whenAlreadyShared() {
		User author = userWithId(User.create("KAKAO", "author", "작성자", null), 1L);
		Review review = reviewWithId(sampleReview(author, "부모님과 다녀온 여행"), 100L);

		given(reviewRepository.findById(100L)).willReturn(Optional.of(review));
		given(communityPostRepository.existsByReview_ReviewIdAndDeletedAtIsNull(100L)).willReturn(true);

		assertThatThrownBy(() -> communityService.createPost(1L, new CreateCommunityPostRequest(100L)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(CommunityErrorCode.POST_ALREADY_SHARED)
			);
	}

	@Test
	void deletePost_success_softDeletesOwnPost() {
		User author = userWithId(User.create("KAKAO", "author", "작성자", null), 1L);
		Review review = reviewWithId(sampleReview(author, "부모님과 다녀온 여행"), 100L);
		CommunityPost post = postWithId(CommunityPost.create(review), 11L, LocalDateTime.of(2026, 9, 13, 11, 0));

		given(communityPostRepository.findActiveById(11L)).willReturn(Optional.of(post));

		communityService.deletePost(1L, 11L);

		assertThat(post.getDeletedAt()).isNotNull();
	}

	@Test
	void deletePost_fail_whenNotOwner() {
		User author = userWithId(User.create("KAKAO", "author", "작성자", null), 2L);
		Review review = reviewWithId(sampleReview(author, "부모님과 다녀온 여행"), 100L);
		CommunityPost post = postWithId(CommunityPost.create(review), 11L, LocalDateTime.of(2026, 9, 13, 11, 0));

		given(communityPostRepository.findActiveById(11L)).willReturn(Optional.of(post));

		assertThatThrownBy(() -> communityService.deletePost(1L, 11L))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(CommunityErrorCode.POST_ACCESS_DENIED)
			);
	}

	@Test
	void likePost_success_increasesLikeCountOnce() {
		User author = userWithId(User.create("KAKAO", "author", "작성자", null), 2L);
		Review review = reviewWithId(sampleReview(author, "부모님과 다녀온 여행"), 100L);
		CommunityPost post = postWithId(CommunityPost.create(review), 11L, LocalDateTime.of(2026, 9, 13, 11, 0));

		given(communityPostRepository.findActiveById(11L)).willReturn(Optional.of(post));
		given(communityPostLikeRepository.insertIgnore(1L, 11L)).willReturn(1);

		var response = communityService.likePost(1L, 11L);

		assertThat(response.liked()).isTrue();
		assertThat(response.likeCount()).isEqualTo(1);
	}

	@Test
	void likePost_doesNotDoubleCount_whenAlreadyLiked() {
		User author = userWithId(User.create("KAKAO", "author", "작성자", null), 2L);
		Review review = reviewWithId(sampleReview(author, "부모님과 다녀온 여행"), 100L);
		CommunityPost post = postWithId(CommunityPost.create(review), 11L, LocalDateTime.of(2026, 9, 13, 11, 0));
		post.increaseLikeCount();

		given(communityPostRepository.findActiveById(11L)).willReturn(Optional.of(post));
		given(communityPostLikeRepository.insertIgnore(1L, 11L)).willReturn(0);

		var response = communityService.likePost(1L, 11L);

		assertThat(response.liked()).isTrue();
		assertThat(response.likeCount()).isEqualTo(1);
	}

	@Test
	void unlikePost_success_decreasesLikeCount() {
		User author = userWithId(User.create("KAKAO", "author", "작성자", null), 2L);
		Review review = reviewWithId(sampleReview(author, "부모님과 다녀온 여행"), 100L);
		CommunityPost post = postWithId(CommunityPost.create(review), 11L, LocalDateTime.of(2026, 9, 13, 11, 0));
		post.increaseLikeCount();

		given(communityPostRepository.findActiveById(11L)).willReturn(Optional.of(post));
		given(communityPostLikeRepository.deleteByUserIdAndPostId(1L, 11L)).willReturn(1);

		var response = communityService.unlikePost(1L, 11L);

		assertThat(response.liked()).isFalse();
		assertThat(response.likeCount()).isEqualTo(0);
	}

	@Test
	void getComments_returnsPage() {
		User author = userWithId(User.create("KAKAO", "author", "작성자", null), 2L);
		Review review = reviewWithId(sampleReview(author, "추천 여행 후기"), 100L);
		CommunityPost post = postWithId(CommunityPost.create(review), 11L, LocalDateTime.of(2026, 9, 13, 11, 0));
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
		Review review = reviewWithId(sampleReview(author, "추천 여행 후기"), 100L);
		CommunityPost post = postWithId(CommunityPost.create(review), 11L, LocalDateTime.of(2026, 9, 13, 11, 0));
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
		Review review = reviewWithId(sampleReview(author, "추천 여행 후기"), 100L);
		CommunityPost post = postWithId(CommunityPost.create(review), 11L, LocalDateTime.of(2026, 9, 13, 11, 0));

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
		Review review = reviewWithId(sampleReview(author, "추천 여행 후기"), 100L);
		CommunityPost post = postWithId(CommunityPost.create(review), 11L, LocalDateTime.of(2026, 9, 13, 11, 0));
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

	private Review sampleReview(User author, String courseTitle) {
		return sampleReview(author, "대전", courseTitle);
	}

	private Review sampleReview(User author, String regionName, String courseTitle) {
		Region region = Region.create("1", "1", regionName);
		Course course = Course.create(author, region, courseTitle, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3));
		return Review.create(course, author, (byte) 5, "가족 여행에 좋았습니다.", (byte) 10);
	}

	private User userWithId(User user, Long userId) {
		return setField(user, "userId", userId);
	}

	private Review reviewWithId(Review review, Long reviewId) {
		return setField(review, "reviewId", reviewId);
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
		return setField(report, "reportId", reportId);
	}

	private CommunityUserBlock blockWithId(CommunityUserBlock block, Long blockId) {
		return setField(block, "blockId", blockId);
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
