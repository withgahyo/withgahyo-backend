package com.withgahyo.domain.auth.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

final class OAuthHttpSupport {

	static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
	static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

	private OAuthHttpSupport() {
	}

	static HttpClient newHttpClient() {
		return HttpClient.newBuilder()
			.connectTimeout(CONNECT_TIMEOUT)
			.build();
	}

	static String textOrDefault(JsonNode node, String defaultValue) {
		String value = textOrNull(node);
		return value == null ? defaultValue : value;
	}

	static String textOrNull(JsonNode node) {
		if (node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
			return null;
		}
		return node.asText();
	}

	static String formBody(String... keyValues) {
		StringBuilder builder = new StringBuilder();
		for (int index = 0; index < keyValues.length; index += 2) {
			if (index > 0) {
				builder.append("&");
			}
			builder.append(encode(keyValues[index])).append("=").append(encode(keyValues[index + 1]));
		}
		return builder.toString();
	}

	static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}
}
