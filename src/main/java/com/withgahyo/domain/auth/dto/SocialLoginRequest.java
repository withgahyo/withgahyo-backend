package com.withgahyo.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SocialLoginRequest(
	@NotBlank(message = "OAuth Access Token은 필수입니다.")
	String oauthAccessToken
) {
}
