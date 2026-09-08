package com.withgahyo.infra.ai;

import com.withgahyo.infra.ai.dto.AiRecommendationGenerateRequest;
import com.withgahyo.infra.ai.dto.AiRecommendationGenerateResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpRecommendationAiClient implements RecommendationAiClient {

	private static final Logger log = LoggerFactory.getLogger(HttpRecommendationAiClient.class);
	private static final String GENERATE_PATH = "/internal/v1/recommendations/generate";

	private final RestClient restClient;

	public HttpRecommendationAiClient(
		@Value("${external.ai-server.base-url:http://localhost:8001}") String baseUrl
	) {
		this.restClient = RestClient.builder()
			.baseUrl(baseUrl)
			// TODO(디버깅용, 원인 파악 후 제거): 실제 wire 요청/응답을 그대로 로그로 남긴다.
			.requestFactory(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()))
			.requestInterceptor((req, body, execution) -> {
				log.info(
					"[AI-DEBUG] POST {} body={}",
					req.getURI(),
					new String(body, StandardCharsets.UTF_8)
				);
				var response = execution.execute(req, body);
				try {
					String responseBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
					log.info("[AI-DEBUG] response status={} body={}", response.getStatusCode(), responseBody);
				} catch (IOException e) {
					log.warn("[AI-DEBUG] failed to read response body for logging", e);
				}
				return response;
			})
			.build();
	}

	@Override
	public AiRecommendationGenerateResponse generate(AiRecommendationGenerateRequest request) {
		try {
			return restClient.post()
				.uri(GENERATE_PATH)
				.body(request)
				.retrieve()
				.body(AiRecommendationGenerateResponse.class);
		} catch (RestClientException exception) {
			// TODO(디버깅용, 원인 파악 후 제거)
			log.info(
				"[AI-DEBUG] RestClientException class={}, message={}, cause={}",
				exception.getClass().getName(),
				exception.getMessage(),
				exception.getCause() == null ? null : exception.getCause().getMessage()
			);
			throw new RecommendationAiException("AI 서버 호출에 실패했습니다.", exception);
		}
	}
}
