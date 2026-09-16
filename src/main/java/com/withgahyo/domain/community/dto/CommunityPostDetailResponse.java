package com.withgahyo.domain.community.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CommunityPostDetailResponse(
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
	String content,
	int commentCount,
	int likeCount,
	boolean likedByMe,
	LocalDateTime createdAt
) {
}
