package com.withgahyo.domain.community.service;

import com.withgahyo.domain.community.dto.BlockCommunityUserResponse;
import com.withgahyo.domain.community.dto.CommunityCommentListResponse;
import com.withgahyo.domain.community.dto.CommunityPostListResponse;
import com.withgahyo.domain.community.dto.CommunityPostDetailResponse;
import com.withgahyo.domain.community.dto.CommunityPostShareUrlResponse;
import com.withgahyo.domain.community.dto.CreateCommunityCommentRequest;
import com.withgahyo.domain.community.dto.CreateCommunityCommentResponse;
import com.withgahyo.domain.community.dto.ReportCommunityPostRequest;
import com.withgahyo.domain.community.dto.ReportCommunityPostResponse;
import com.withgahyo.domain.community.entity.CommunityComment;
import com.withgahyo.domain.community.entity.CommunityPost;
import com.withgahyo.domain.community.entity.CommunityPostReport;
import com.withgahyo.domain.community.entity.CommunityUserBlock;
import com.withgahyo.domain.community.exception.CommunityErrorCode;
import com.withgahyo.domain.community.repository.CommunityCommentRepository;
import com.withgahyo.domain.community.repository.CommunityPostRepository;
import com.withgahyo.domain.community.repository.CommunityPostReportRepository;
import com.withgahyo.domain.community.repository.CommunityUserBlockRepository;
import com.withgahyo.domain.user.entity.User;
import com.withgahyo.domain.user.repository.UserRepository;
import com.withgahyo.global.exception.BusinessException;
import com.withgahyo.global.exception.code.SecurityErrorCode;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityService {

	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 50;
	private static final int DEFAULT_RECOMMENDATION_SIZE = 5;

	private final CommunityPostRepository communityPostRepository;
	private final CommunityCommentRepository communityCommentRepository;
	private final UserRepository userRepository;
	private final CommunityPostReportRepository communityPostReportRepository;
	private final CommunityUserBlockRepository communityUserBlockRepository;

	public CommunityService(
		CommunityPostRepository communityPostRepository,
		CommunityCommentRepository communityCommentRepository,
		UserRepository userRepository,
		CommunityPostReportRepository communityPostReportRepository,
		CommunityUserBlockRepository communityUserBlockRepository
	) {
		this.communityPostRepository = communityPostRepository;
		this.communityCommentRepository = communityCommentRepository;
		this.userRepository = userRepository;
		this.communityPostReportRepository = communityPostReportRepository;
		this.communityUserBlockRepository = communityUserBlockRepository;
	}

	@Transactional(readOnly = true)
	public CommunityPostListResponse getPosts(
		Long userId,
		String keyword,
		String category,
		String sort,
		Long cursor,
		Integer size
	) {
		int pageSize = normalizeSize(size, DEFAULT_PAGE_SIZE);
		List<CommunityPost> posts = communityPostRepository.searchActivePosts(
			normalizeText(keyword),
			normalizeText(category),
			normalizeSort(sort),
			cursor,
			pageSize + 1
		);

		boolean hasNext = posts.size() > pageSize;
		List<CommunityPost> pagePosts = hasNext ? posts.subList(0, pageSize) : posts;

		return toPostListResponse(pagePosts, hasNext);
	}

	@Transactional(readOnly = true)
	public CommunityPostListResponse getRecommendedPosts(Long userId, Integer size) {
		List<CommunityPost> posts = communityPostRepository.findRecommendedActivePosts(
			normalizeSize(size, DEFAULT_RECOMMENDATION_SIZE)
		);
		return toPostListResponse(posts, false);
	}

	@Transactional(readOnly = true)
	public CommunityPostDetailResponse getPostDetail(Long userId, Long postId) {
		CommunityPost post = findActivePost(postId);
		return new CommunityPostDetailResponse(
			post.getPostId(),
			post.getAuthor().getUserId(),
			post.getAuthor().getNickname(),
			post.getCategory(),
			post.getTitle(),
			post.getContent(),
			communityCommentRepository.countActiveCommentsByPostId(postId).intValue(),
			post.getLikeCount(),
			post.getCreatedAt()
		);
	}

	@Transactional(readOnly = true)
	public CommunityPostShareUrlResponse getPostShareUrl(Long userId, Long postId) {
		CommunityPost post = findActivePost(postId);
		return new CommunityPostShareUrlResponse(
			post.getPostId(),
			"https://api.gatigahyo.com/community/posts/" + post.getPostId()
		);
	}

	@Transactional(readOnly = true)
	public CommunityCommentListResponse getComments(Long userId, Long postId, Long cursor, Integer size) {
		findActivePost(postId);
		int pageSize = normalizeSize(size, DEFAULT_PAGE_SIZE);
		List<CommunityComment> comments = communityCommentRepository.findActiveCommentsByPostId(
			postId,
			cursor,
			pageSize + 1
		);
		boolean hasNext = comments.size() > pageSize;
		List<CommunityComment> pageComments = hasNext ? comments.subList(0, pageSize) : comments;

		return new CommunityCommentListResponse(
			pageComments.stream()
				.map(this::toCommentResponse)
				.toList(),
			hasNext,
			hasNext && !pageComments.isEmpty()
				? String.valueOf(pageComments.get(pageComments.size() - 1).getCommentId())
				: null
		);
	}

	@Transactional
	public CreateCommunityCommentResponse createComment(
		Long userId,
		Long postId,
		CreateCommunityCommentRequest request
	) {
		CommunityPost post = findActivePost(postId);
		User author = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(SecurityErrorCode.INVALID_TOKEN));
		CommunityComment comment = communityCommentRepository.save(CommunityComment.create(
			post,
			author,
			request.normalizedContent()
		));
		return new CreateCommunityCommentResponse(
			comment.getCommentId(),
			comment.getPost().getPostId(),
			comment.getAuthor().getUserId(),
			comment.getContent(),
			comment.getCreatedAt()
		);
	}

	@Transactional
	public ReportCommunityPostResponse reportPost(
		Long userId,
		Long postId,
		ReportCommunityPostRequest request
	) {
		CommunityPost post = findActivePost(postId);
		User reporter = findUser(userId);
		CommunityPostReport report = communityPostReportRepository.save(CommunityPostReport.create(
			post,
			reporter,
			request.normalizedReason(),
			request.normalizedDescription()
		));

		return new ReportCommunityPostResponse(
			report.getReportId(),
			report.getPost().getPostId(),
			report.getReason()
		);
	}

	@Transactional
	public BlockCommunityUserResponse blockUser(Long requesterUserId, Long blockedUserId) {
		if (requesterUserId.equals(blockedUserId)) {
			throw new BusinessException(CommunityErrorCode.SELF_BLOCK_NOT_ALLOWED);
		}
		User blocker = findUser(requesterUserId);
		User blockedUser = findUser(blockedUserId);
		CommunityUserBlock block = communityUserBlockRepository.save(CommunityUserBlock.create(blocker, blockedUser));

		return new BlockCommunityUserResponse(block.getBlockId(), block.getBlockedUser().getUserId());
	}

	private CommunityPostListResponse toPostListResponse(List<CommunityPost> posts, boolean hasNext) {
		List<Long> postIds = posts.stream()
			.map(CommunityPost::getPostId)
			.toList();
		Map<Long, Long> commentCounts = communityCommentRepository.countActiveCommentsByPostIds(postIds);

		return new CommunityPostListResponse(
			posts.stream()
				.map(post -> toPostSummaryResponse(post, commentCounts))
				.toList(),
			hasNext,
			hasNext && !posts.isEmpty() ? String.valueOf(posts.get(posts.size() - 1).getPostId()) : null
		);
	}

	private CommunityPostListResponse.PostSummaryResponse toPostSummaryResponse(
		CommunityPost post,
		Map<Long, Long> commentCounts
	) {
		return new CommunityPostListResponse.PostSummaryResponse(
			post.getPostId(),
			post.getAuthor().getUserId(),
			post.getAuthor().getNickname(),
			post.getCategory(),
			post.getTitle(),
			toPreview(post.getContent()),
			commentCounts.getOrDefault(post.getPostId(), 0L).intValue(),
			post.getLikeCount(),
			post.getCreatedAt()
		);
	}

	private String toPreview(String content) {
		if (content.length() <= 80) {
			return content;
		}
		return content.substring(0, 80);
	}

	private int normalizeSize(Integer size, int defaultSize) {
		if (size == null || size < 1) {
			return defaultSize;
		}
		return Math.min(size, MAX_PAGE_SIZE);
	}

	private String normalizeText(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private String normalizeSort(String sort) {
		if (sort == null || sort.isBlank()) {
			return "latest";
		}
		return sort.trim();
	}

	private CommunityPost findActivePost(Long postId) {
		return communityPostRepository.findActiveById(postId)
			.orElseThrow(() -> new BusinessException(CommunityErrorCode.POST_NOT_FOUND));
	}

	private User findUser(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(SecurityErrorCode.INVALID_TOKEN));
	}

	private CommunityCommentListResponse.CommentResponse toCommentResponse(CommunityComment comment) {
		return new CommunityCommentListResponse.CommentResponse(
			comment.getCommentId(),
			comment.getAuthor().getUserId(),
			comment.getAuthor().getNickname(),
			comment.getContent(),
			comment.getCreatedAt()
		);
	}
}
