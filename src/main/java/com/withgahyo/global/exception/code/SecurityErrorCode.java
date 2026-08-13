package com.withgahyo.global.exception.code;

import org.springframework.http.HttpStatus;

public enum SecurityErrorCode implements ErrorCode {
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_401_001", "인증이 필요합니다."),
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_002", "유효하지 않은 토큰입니다."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_403_001", "접근 권한이 없습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	SecurityErrorCode(HttpStatus httpStatus, String code, String message) {
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
