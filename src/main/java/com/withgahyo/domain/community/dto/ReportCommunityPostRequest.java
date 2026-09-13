package com.withgahyo.domain.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReportCommunityPostRequest(
	@NotBlank(message = "신고 사유는 필수입니다.")
	@Size(max = 30, message = "신고 사유는 30자 이하여야 합니다.")
	String reason,

	@Size(max = 500, message = "신고 상세 내용은 500자 이하여야 합니다.")
	String description
) {

	public String normalizedReason() {
		return reason == null ? "" : reason.trim();
	}

	public String normalizedDescription() {
		if (description == null || description.isBlank()) {
			return null;
		}
		return description.trim();
	}
}
