package com.withgahyo.domain.auth.dto;

public record AuthTokenResponse(
	String accessToken,
	String refreshToken,
	String tokenType,
	long expiresIn,
	boolean isNewUser,
	AuthUserResponse user
) {

	private static final String TOKEN_TYPE = "Bearer";

	public static AuthTokenResponse of(
		String accessToken,
		String refreshToken,
		long expiresIn,
		boolean isNewUser,
		AuthUserResponse user
	) {
		return new AuthTokenResponse(accessToken, refreshToken, TOKEN_TYPE, expiresIn, isNewUser, user);
	}
}
