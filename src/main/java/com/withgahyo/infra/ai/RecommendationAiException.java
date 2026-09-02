package com.withgahyo.infra.ai;

public class RecommendationAiException extends RuntimeException {

	public RecommendationAiException(String message) {
		super(message);
	}

	public RecommendationAiException(String message, Throwable cause) {
		super(message, cause);
	}
}
