package com.withgahyo.domain.place.exception;

import com.withgahyo.global.exception.code.ErrorCode;
import org.springframework.http.HttpStatus;

public enum PlaceErrorCode implements ErrorCode {
	REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "PLACE_404_001", "지역을 찾을 수 없습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	PlaceErrorCode(HttpStatus httpStatus, String code, String message) {
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
