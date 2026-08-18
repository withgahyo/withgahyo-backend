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
public class GoogleOAuthUserClient implements OAuthUserClient {

	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;
	private final String userInfoUri;

	@Autowired
	public GoogleOAuthUserClient(
		ObjectMapper objectMapper,
		@Value("${auth.oauth.google.user-info-uri:https://www.googleapis.com/oauth2/v3/userinfo}") String userInfoUri
	) {
		this(HttpClient.newHttpClient(), objectMapper, userInfoUri);
	}

	GoogleOAuthUserClient(HttpClient httpClient, ObjectMapper objectMapper, String userInfoUri) {
		this.httpClient = httpClient;
		this.objectMapper = objectMapper;
		this.userInfoUri = userInfoUri;
	}

	@Override
	public OAuthProvider getProvider() {
		return OAuthProvider.GOOGLE;
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
		JsonNode subjectNode = root.path("sub");
		if (subjectNode.isMissingNode() || subjectNode.isNull() || subjectNode.asText().isBlank()) {
			throw new BusinessException(SecurityErrorCode.INVALID_TOKEN);
		}

		String nickname = textOrDefault(root.path("name"), "같이가효");
		String profileImageUrl = textOrNull(root.path("picture"));
		return new OAuthUserInfo(OAuthProvider.GOOGLE, subjectNode.asText(), nickname, profileImageUrl);
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
