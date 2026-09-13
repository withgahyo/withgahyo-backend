package com.withgahyo.domain.community.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CommunityPostListResponse(
	List<PostSummaryResponse> posts,
	boolean hasNext,
	String nextCursor
) {

	public record PostSummaryResponse(
		Long postId,
		Long authorId,
		String authorNickname,
		String category,
		String title,
		String contentPreview,
		int commentCount,
		int likeCount,
		LocalDateTime createdAt
	) {
	}
}
