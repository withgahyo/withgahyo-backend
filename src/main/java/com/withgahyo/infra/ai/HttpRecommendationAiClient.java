package com.withgahyo.infra.ai;

import com.withgahyo.infra.ai.dto.AiRecommendationGenerateRequest;
import com.withgahyo.infra.ai.dto.AiRecommendationGenerateResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpRecommendationAiClient implements RecommendationAiClient {

	private static final String GENERATE_PATH = "/internal/v1/recommendations/generate";
	private static final String HEALTH_PATH = "/health";
	private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
	private static final Duration DEFAULT_READY_TIMEOUT = Duration.ofSeconds(90);
	private static final Duration DEFAULT_RETRY_INTERVAL = Duration.ofSeconds(2);

	private final RestClient restClient;
	private final RestClient healthClient;
	private final Duration readyTimeout;
	private final Duration retryInterval;

	@Autowired
	public HttpRecommendationAiClient(
		@Value("${external.ai-server.base-url:http://localhost:8001}") String baseUrl,
		@Value("${internal.ai.api-key}") String internalApiKey
	) {
		this(baseUrl, internalApiKey, DEFAULT_READY_TIMEOUT, DEFAULT_RETRY_INTERVAL);
	}

	HttpRecommendationAiClient(
		String baseUrl,
		String internalApiKey,
		Duration readyTimeout,
		Duration retryInterval
	) {
		this.restClient = RestClient.builder()
			.baseUrl(baseUrl)
			.defaultHeader(INTERNAL_API_KEY_HEADER, internalApiKey)
			.build();
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(5));
		requestFactory.setReadTimeout(Duration.ofSeconds(10));
		this.healthClient = RestClient.builder()
			.baseUrl(baseUrl)
			.requestFactory(requestFactory)
			.build();
		this.readyTimeout = readyTimeout;
		this.retryInterval = retryInterval;
	}

	@Override
	public AiRecommendationGenerateResponse generate(AiRecommendationGenerateRequest request) {
		awaitReady();
		try {
			return restClient.post()
				.uri(GENERATE_PATH)
				.body(request)
				.retrieve()
				.body(AiRecommendationGenerateResponse.class);
		} catch (RestClientException exception) {
			throw new RecommendationAiException("AI 서버 호출에 실패했습니다.", exception);
		}
	}

	private void awaitReady() {
		long deadline = System.nanoTime() + readyTimeout.toNanos();
		while (true) {
			try {
				if (healthClient.get().uri(HEALTH_PATH).retrieve().toBodilessEntity()
					.getStatusCode().is2xxSuccessful()) {
					return;
				}
			} catch (RestClientException ignored) {
				// A sleeping instance may fail or return 5xx while starting up.
			}
			long remaining = deadline - System.nanoTime();
			if (remaining <= 0) {
				throw new RecommendationAiException("AI 서버가 준비되지 않았습니다. 잠시 후 다시 시도해주세요.");
			}
			try {
				Thread.sleep(Math.min(retryInterval.toMillis(), Duration.ofNanos(remaining).toMillis()));
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				throw new RecommendationAiException("AI 서버 준비 대기가 중단되었습니다.", exception);
			}
		}
	}
}
