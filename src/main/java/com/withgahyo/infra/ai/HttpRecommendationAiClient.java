package com.withgahyo.infra.ai;

import com.withgahyo.infra.ai.dto.AiRecommendationGenerateRequest;
import com.withgahyo.infra.ai.dto.AiRecommendationGenerateResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpRecommendationAiClient implements RecommendationAiClient {

	private static final String GENERATE_PATH = "/internal/v1/recommendations/generate";
	private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

	private final RestClient restClient;

	public HttpRecommendationAiClient(
		@Value("${external.ai-server.base-url:http://localhost:8001}") String baseUrl,
		@Value("${internal.ai.api-key}") String internalApiKey
	) {
		this.restClient = RestClient.builder()
			.baseUrl(baseUrl)
			.defaultHeader(INTERNAL_API_KEY_HEADER, internalApiKey)
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
			throw new RecommendationAiException("AI 서버 호출에 실패했습니다.", exception);
		}
	}
}
