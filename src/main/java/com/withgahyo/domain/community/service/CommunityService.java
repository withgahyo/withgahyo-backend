package com.withgahyo.domain.community.service;

import com.withgahyo.domain.community.dto.CommunityPostListResponse;
import com.withgahyo.domain.community.dto.CommunityPostDetailResponse;
import com.withgahyo.domain.community.dto.CommunityPostShareUrlResponse;
import com.withgahyo.domain.community.entity.CommunityPost;
import com.withgahyo.domain.community.exception.CommunityErrorCode;
import com.withgahyo.domain.community.repository.CommunityCommentRepository;
import com.withgahyo.domain.community.repository.CommunityPostRepository;
import com.withgahyo.global.exception.BusinessException;
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

	public CommunityService(
		CommunityPostRepository communityPostRepository,
		CommunityCommentRepository communityCommentRepository
	) {
		this.communityPostRepository = communityPostRepository;
		this.communityCommentRepository = communityCommentRepository;
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
}
