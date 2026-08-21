package com.withgahyo.domain.auth.dto;

public record TokenRefreshResponse(
	String accessToken,
	String refreshToken,
	String tokenType,
	long expiresIn
) {

	private static final String TOKEN_TYPE = "Bearer";

	public static TokenRefreshResponse of(String accessToken, String refreshToken, long expiresIn) {
		return new TokenRefreshResponse(accessToken, refreshToken, TOKEN_TYPE, expiresIn);
	}
}
