package com.withgahyo.domain.community.dto;

import java.time.LocalDateTime;

public record CommunityPostDetailResponse(
	Long postId,
	Long authorId,
	String authorNickname,
	String category,
	String title,
	String content,
	int commentCount,
	int likeCount,
	LocalDateTime createdAt
) {
}
