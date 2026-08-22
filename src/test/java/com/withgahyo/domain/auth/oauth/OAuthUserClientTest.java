package com.withgahyo.domain.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OAuthUserClientTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final List<RecordedRequest> recordedRequests = new ArrayList<>();
	private HttpServer server;

	@AfterEach
	void tearDown() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	void 카카오_인가_코드로_토큰을_발급받고_사용자_정보를_조회한다() throws IOException {
		String baseUrl = startServer(
			"{\"access_token\":\"kakao-provider-access-token\"}",
			"{\"id\":12345,\"properties\":{\"nickname\":\"카카오닉네임\",\"profile_image\":\"https://example.com/kakao.png\"}}"
		);
		KakaoOAuthUserClient client = new KakaoOAuthUserClient(
			HttpClient.newHttpClient(),
			objectMapper,
			baseUrl + "/token",
			baseUrl + "/userinfo",
			"kakao-client-id",
			"kakao-secret"
		);

		OAuthUserInfo userInfo = client.getUserInfo(
			"kakao-code",
			"http://localhost:5173/oauth/kakao/callback"
		);

		assertThat(userInfo.provider()).isEqualTo(OAuthProvider.KAKAO);
		assertThat(userInfo.providerUserId()).isEqualTo("12345");
		assertThat(userInfo.nickname()).isEqualTo("카카오닉네임");
		assertThat(userInfo.profileImageUrl()).isEqualTo("https://example.com/kakao.png");
		assertThat(recordedRequests.get(0).method()).isEqualTo("POST");
		assertThat(recordedRequests.get(0).body()).contains(
			"grant_type=authorization_code",
			"client_id=kakao-client-id",
			"code=kakao-code",
			"redirect_uri=http%3A%2F%2Flocalhost%3A5173%2Foauth%2Fkakao%2Fcallback",
			"client_secret=kakao-secret"
		);
		assertThat(recordedRequests.get(1).authorization()).isEqualTo("Bearer kakao-provider-access-token");
	}

	@Test
	void 구글_인가_코드로_토큰을_발급받고_사용자_정보를_조회한다() throws IOException {
		String baseUrl = startServer(
			"{\"access_token\":\"google-provider-access-token\"}",
			"{\"sub\":\"google-123\",\"email\":\"google@example.com\",\"name\":\"구글닉네임\",\"picture\":\"https://example.com/google.png\"}"
		);
		GoogleOAuthUserClient client = new GoogleOAuthUserClient(
			HttpClient.newHttpClient(),
			objectMapper,
			baseUrl + "/token",
			baseUrl + "/userinfo",
			"google-client-id",
			"google-secret"
		);

		OAuthUserInfo userInfo = client.getUserInfo(
			"google-code",
			"http://localhost:5173/oauth/google/callback"
		);

		assertThat(userInfo.provider()).isEqualTo(OAuthProvider.GOOGLE);
		assertThat(userInfo.providerUserId()).isEqualTo("google-123");
		assertThat(userInfo.email()).isEqualTo("google@example.com");
		assertThat(userInfo.nickname()).isEqualTo("구글닉네임");
		assertThat(userInfo.profileImageUrl()).isEqualTo("https://example.com/google.png");
		assertThat(recordedRequests.get(0).method()).isEqualTo("POST");
		assertThat(recordedRequests.get(0).body()).contains(
			"grant_type=authorization_code",
			"client_id=google-client-id",
			"client_secret=google-secret",
			"code=google-code",
			"redirect_uri=http%3A%2F%2Flocalhost%3A5173%2Foauth%2Fgoogle%2Fcallback"
		);
		assertThat(recordedRequests.get(1).authorization()).isEqualTo("Bearer google-provider-access-token");
	}

	private String startServer(String tokenResponse, String userInfoResponse) throws IOException {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/token", exchange -> respond(exchange, tokenResponse));
		server.createContext("/userinfo", exchange -> respond(exchange, userInfoResponse));
		server.start();
		return "http://localhost:" + server.getAddress().getPort();
	}

	private void respond(HttpExchange exchange, String responseBody) throws IOException {
		recordedRequests.add(new RecordedRequest(
			exchange.getRequestMethod(),
			exchange.getRequestHeaders().getFirst("Authorization"),
			new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)
		));
		byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json");
		exchange.sendResponseHeaders(200, response.length);
		exchange.getResponseBody().write(response);
		exchange.close();
	}

	private record RecordedRequest(String method, String authorization, String body) {
	}
}
