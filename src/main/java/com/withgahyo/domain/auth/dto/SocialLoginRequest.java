package com.withgahyo.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SocialLoginRequest(
	@NotBlank(message = "Authorization Code는 필수입니다.")
	String authorizationCode,

	@NotBlank(message = "Redirect URI는 필수입니다.")
	String redirectUri
) {
}
