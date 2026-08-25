package com.withgahyo.domain.auth.oauth;

public record OAuthUserInfo(
	OAuthProvider provider,
	String providerUserId,
	String nickname,
	String profileImageUrl,
	String email
) {
	public OAuthUserInfo(OAuthProvider provider, String providerUserId, String nickname, String profileImageUrl) {
		this(provider, providerUserId, nickname, profileImageUrl, null);
	}
}
