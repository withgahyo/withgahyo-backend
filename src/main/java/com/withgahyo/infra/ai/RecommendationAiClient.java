package com.withgahyo.infra.ai;

import com.withgahyo.infra.ai.dto.AiRecommendationGenerateRequest;
import com.withgahyo.infra.ai.dto.AiRecommendationGenerateResponse;

public interface RecommendationAiClient {

	AiRecommendationGenerateResponse generate(AiRecommendationGenerateRequest request);
}
