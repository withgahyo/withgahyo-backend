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
	private final String userInfoUri;

	@Autowired
	public KakaoOAuthUserClient(
		ObjectMapper objectMapper,
		@Value("${auth.oauth.kakao.user-info-uri:https://kapi.kakao.com/v2/user/me}") String userInfoUri
	) {
		this(HttpClient.newHttpClient(), objectMapper, userInfoUri);
	}

	KakaoOAuthUserClient(HttpClient httpClient, ObjectMapper objectMapper, String userInfoUri) {
		this.httpClient = httpClient;
		this.objectMapper = objectMapper;
		this.userInfoUri = userInfoUri;
	}

	@Override
	public OAuthProvider getProvider() {
		return OAuthProvider.KAKAO;
	}

	@Override
	public OAuthUserInfo getUserInfo(String oauthAccessToken) {
		HttpRequest request = HttpRequest.newBuilder(URI.create(userInfoUri))
			.header("Authorization", "Bearer " + oauthAccessToken)
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

	private OAuthUserInfo parse(String responseBody) throws IOException {
		JsonNode root = objectMapper.readTree(responseBody);
		JsonNode idNode = root.path("id");
		if (idNode.isMissingNode() || idNode.isNull()) {
			throw new BusinessException(SecurityErrorCode.INVALID_TOKEN);
		}

		JsonNode properties = root.path("properties");
		String nickname = textOrDefault(properties.path("nickname"), "같이가효");
		String profileImageUrl = textOrNull(properties.path("profile_image"));

		return new OAuthUserInfo(OAuthProvider.KAKAO, idNode.asText(), nickname, profileImageUrl);
	}

	private String textOrDefault(JsonNode node, String defaultValue) {
		String value = textOrNull(node);
		return value == null ? defaultValue : value;
	}

	private String textOrNull(JsonNode node) {
		if (node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
			return null;
		}
		return node.asText();
	}
}
