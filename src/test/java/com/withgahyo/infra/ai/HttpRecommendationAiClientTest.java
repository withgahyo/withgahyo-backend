package com.withgahyo.infra.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpServer;
import com.withgahyo.infra.ai.dto.AiRecommendationGenerateRequest;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class HttpRecommendationAiClientTest {

	@Test
	void sendsInternalApiKeyToAiServer() throws IOException {
		AtomicReference<String> receivedKey = new AtomicReference<>();
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/internal/v1/recommendations/generate", exchange -> {
			receivedKey.set(exchange.getRequestHeaders().getFirst("X-Internal-Api-Key"));
			byte[] body = "{\"generationId\":1,\"courseId\":2,\"candidates\":[]}".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, body.length);
			try (var output = exchange.getResponseBody()) {
				output.write(body);
			}
		});
		server.start();
		try {
			var client = new HttpRecommendationAiClient(
				"http://127.0.0.1:" + server.getAddress().getPort(), "test-internal-key"
			);
			var response = client.generate(new AiRecommendationGenerateRequest(1L, 2L, null, List.of()));

			assertEquals("test-internal-key", receivedKey.get());
			assertEquals(1L, response.generationId());
		} finally {
			server.stop(0);
		}
	}
}
