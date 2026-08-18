package com.withgahyo.domain.auth.oauth;

public interface OAuthUserClient {

	OAuthProvider getProvider();

	OAuthUserInfo getUserInfo(String oauthAccessToken);
}
