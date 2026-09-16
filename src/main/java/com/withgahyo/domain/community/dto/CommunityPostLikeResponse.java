package com.withgahyo.domain.community.dto;

public record CommunityPostLikeResponse(
	Long postId,
	boolean liked,
	int likeCount
) {

	public static CommunityPostLikeResponse of(Long postId, boolean liked, int likeCount) {
		return new CommunityPostLikeResponse(postId, liked, likeCount);
	}
}
