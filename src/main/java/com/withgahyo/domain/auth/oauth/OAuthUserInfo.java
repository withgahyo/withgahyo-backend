package com.withgahyo.domain.auth.oauth;

public record OAuthUserInfo(
	OAuthProvider provider,
	String providerUserId,
	String email,
	String nickname,
	String profileImageUrl
) {

	public OAuthUserInfo(
		OAuthProvider provider,
		String providerUserId,
		String nickname,
		String profileImageUrl
	) {
		this(provider, providerUserId, null, nickname, profileImageUrl);
	}
}
