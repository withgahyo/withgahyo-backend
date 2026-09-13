package com.withgahyo.domain.community.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CommunityCommentListResponse(
	List<CommentResponse> comments,
	boolean hasNext,
	String nextCursor
) {

	public record CommentResponse(
		Long commentId,
		Long authorId,
		String authorNickname,
		String content,
		LocalDateTime createdAt
	) {
	}
}
