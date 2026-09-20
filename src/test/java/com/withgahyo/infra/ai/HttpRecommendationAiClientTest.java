package com.withgahyo.infra.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpServer;
import com.withgahyo.infra.ai.dto.AiRecommendationGenerateRequest;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class HttpRecommendationAiClientTest {
	private static final AiRecommendationGenerateRequest REQUEST =
		new AiRecommendationGenerateRequest(1L, 2L, null, List.of());

	@Test
	void sendsInternalApiKeyToAiServer() throws IOException {
		AtomicReference<String> receivedKey = new AtomicReference<>();
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/health", exchange -> {
			exchange.sendResponseHeaders(200, -1);
			exchange.close();
		});
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

	@Test
	void waitsForAiServerToBecomeReadyBeforeGenerating() throws IOException {
		AtomicInteger healthChecks = new AtomicInteger();
		AtomicInteger generations = new AtomicInteger();
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/health", exchange -> {
			int status = healthChecks.incrementAndGet() == 1 ? 503 : 200;
			exchange.sendResponseHeaders(status, -1);
			exchange.close();
		});
		server.createContext("/internal/v1/recommendations/generate", exchange -> {
			generations.incrementAndGet();
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
				"http://127.0.0.1:" + server.getAddress().getPort(), "test-internal-key",
				Duration.ofSeconds(1), Duration.ofMillis(10)
			);
			assertEquals(1L, client.generate(REQUEST).generationId());
			assertEquals(2, healthChecks.get());
			assertEquals(1, generations.get());
		} finally {
			server.stop(0);
		}
	}

	@Test
	void doesNotGenerateWhenAiServerNeverBecomesReady() throws IOException {
		AtomicInteger generations = new AtomicInteger();
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/health", exchange -> {
			exchange.sendResponseHeaders(503, -1);
			exchange.close();
		});
		server.createContext("/internal/v1/recommendations/generate", exchange -> {
			generations.incrementAndGet();
			exchange.sendResponseHeaders(200, -1);
			exchange.close();
		});
		server.start();
		try {
			var client = new HttpRecommendationAiClient(
				"http://127.0.0.1:" + server.getAddress().getPort(), "test-internal-key",
				Duration.ofMillis(60), Duration.ofMillis(10)
			);
			assertThrows(RecommendationAiException.class, () -> client.generate(REQUEST));
			assertEquals(0, generations.get());
		} finally {
			server.stop(0);
		}
	}
}
