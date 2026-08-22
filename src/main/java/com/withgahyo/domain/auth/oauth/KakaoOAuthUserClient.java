package com.withgahyo.domain.auth.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.withgahyo.global.exception.BusinessException;
import com.withgahyo.global.exception.code.SecurityErrorCode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KakaoOAuthUserClient implements OAuthUserClient {

	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;
	private final String tokenUri;
	private final String userInfoUri;
	private final String clientId;
	private final String clientSecret;

	@Autowired
	public KakaoOAuthUserClient(
		ObjectMapper objectMapper,
		@Value("${auth.oauth.kakao.token-uri:https://kauth.kakao.com/oauth/token}") String tokenUri,
		@Value("${auth.oauth.kakao.user-info-uri:https://kapi.kakao.com/v2/user/me}") String userInfoUri,
		@Value("${auth.oauth.kakao.client-id}") String clientId,
		@Value("${auth.oauth.kakao.client-secret:}") String clientSecret
	) {
		this(OAuthHttpSupport.newHttpClient(), objectMapper, tokenUri, userInfoUri, clientId, clientSecret);
	}

	KakaoOAuthUserClient(
		HttpClient httpClient,
		ObjectMapper objectMapper,
		String tokenUri,
		String userInfoUri,
		String clientId,
		String clientSecret
	) {
		this.httpClient = httpClient;
		this.objectMapper = objectMapper;
		this.tokenUri = tokenUri;
		this.userInfoUri = userInfoUri;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
	}

	@Override
	public OAuthProvider getProvider() {
		return OAuthProvider.KAKAO;
	}

	@Override
	public OAuthUserInfo getUserInfo(String authorizationCode, String redirectUri) {
		OAuthTokenResponse tokenResponse = getToken(authorizationCode, redirectUri);
		HttpRequest request = HttpRequest.newBuilder(URI.create(userInfoUri))
			.timeout(OAuthHttpSupport.REQUEST_TIMEOUT)
			.header("Authorization", "Bearer " + tokenResponse.accessToken())
			.GET()
			.build();

		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				throw new BusinessException(SecurityErrorCode.INVALID_TOKEN);
			}
			return parse(response.body());
		} catch (IOException exception) {
			throw new BusinessException(SecurityErrorCode.INVALID_TOKEN);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new BusinessException(SecurityErrorCode.INVALID_TOKEN);
		}
	}

	private OAuthTokenResponse getToken(String authorizationCode, String redirectUri) {
		String body = OAuthHttpSupport.formBody(
			"grant_type", "authorization_code",
			"client_id", clientId,
			"redirect_uri", redirectUri,
			"code", authorizationCode
		);
		if (!clientSecret.isBlank()) {
			body += "&" + OAuthHttpSupport.encode("client_secret") + "=" + OAuthHttpSupport.encode(clientSecret);
		}

		HttpRequest request = HttpRequest.newBuilder(URI.create(tokenUri))
			.timeout(OAuthHttpSupport.REQUEST_TIMEOUT)
			.header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
			.POST(HttpRequest.BodyPublishers.ofString(body))
			.build();

		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				throw new BusinessException(SecurityErrorCode.INVALID_TOKEN);
			}
			JsonNode root = objectMapper.readTree(response.body());
			String accessToken = OAuthHttpSupport.textOrNull(root.path("access_token"));
			if (accessToken == null) {
				throw new BusinessException(SecurityErrorCode.INVALID_TOKEN);
			}
			return new OAuthTokenResponse(accessToken);
		} catch (IOException exception) {
			throw new BusinessException(SecurityErrorCode.INVALID_TOKEN);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new BusinessException(SecurityErrorCode.INVALID_TOKEN);
		}
	}

	private OAuthUserInfo parse(String responseBody) throws IOException {
		JsonNode root = objectMapper.readTree(responseBody);
		JsonNode idNode = root.path("id");
		if (idNode.isMissingNode() || idNode.isNull()) {
			throw new BusinessException(SecurityErrorCode.INVALID_TOKEN);
		}

		JsonNode properties = root.path("properties");
		JsonNode kakaoAccount = root.path("kakao_account");
		String email = OAuthHttpSupport.textOrNull(kakaoAccount.path("email"));
		String nickname = OAuthHttpSupport.textOrDefault(properties.path("nickname"), "같이가효");
		String profileImageUrl = OAuthHttpSupport.textOrNull(properties.path("profile_image"));

		return new OAuthUserInfo(OAuthProvider.KAKAO, idNode.asText(), email, nickname, profileImageUrl);
	}
}
