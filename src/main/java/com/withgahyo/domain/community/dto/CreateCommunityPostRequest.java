package com.withgahyo.domain.community.dto;

import jakarta.validation.constraints.NotNull;

public record CreateCommunityPostRequest(
	@NotNull(message = "공유할 후기 ID는 필수입니다.")
	Long reviewId
) {
}
