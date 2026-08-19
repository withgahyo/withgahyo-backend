package com.withgahyo.domain.onboarding.exception;

import com.withgahyo.global.exception.code.ErrorCode;
import org.springframework.http.HttpStatus;

public enum OnboardingErrorCode implements ErrorCode {
	INVALID_TOURISM_PREFERENCE(HttpStatus.BAD_REQUEST, "ONBOARDING_400_001", "존재하지 않는 관광 취향입니다."),
	INVALID_FOOD_PREFERENCE(HttpStatus.BAD_REQUEST, "ONBOARDING_400_002", "존재하지 않는 식사 취향입니다."),
	INCOMPLETE_ONBOARDING(HttpStatus.BAD_REQUEST, "ONBOARDING_400_003", "온보딩 필수 항목이 모두 입력되지 않았습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	OnboardingErrorCode(HttpStatus httpStatus, String code, String message) {
		this.httpStatus = httpStatus;
		this.code = code;
		this.message = message;
	}

	@Override
	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	@Override
	public String getCode() {
		return code;
	}

	@Override
	public String getMessage() {
		return message;
	}
}
