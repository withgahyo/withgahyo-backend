package com.withgahyo.domain.community.dto;

import java.time.LocalDateTime;

public record CreateCommunityCommentResponse(
	Long commentId,
	Long postId,
	Long authorId,
	String content,
	LocalDateTime createdAt
) {
}
