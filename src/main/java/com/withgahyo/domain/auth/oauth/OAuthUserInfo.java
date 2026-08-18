package com.withgahyo.domain.auth.oauth;

public record OAuthUserInfo(
	OAuthProvider provider,
	String providerUserId,
	String nickname,
	String profileImageUrl
) {
}
