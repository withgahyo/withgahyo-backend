package com.withgahyo.domain.community.dto;

public record ReportCommunityPostResponse(
	Long reportId,
	Long postId,
	String reason
) {
}
