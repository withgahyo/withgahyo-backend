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
		String authorProfileImageUrl,
		Long courseId,
		String courseTitle,
		String regionName,
		String courseImageUrl,
		Byte rating,
		List<String> highlights,
		String contentPreview,
		int commentCount,
		int likeCount,
		boolean likedByMe,
		LocalDateTime createdAt
	) {
	}
}
