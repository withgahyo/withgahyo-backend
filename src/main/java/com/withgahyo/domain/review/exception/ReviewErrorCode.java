package com.withgahyo.domain.review.exception;

import com.withgahyo.global.exception.code.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ReviewErrorCode implements ErrorCode {
	REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "REVIEW_409_001", "이미 작성한 후기입니다."),
	COURSE_NOT_REVIEWABLE(HttpStatus.CONFLICT, "REVIEW_409_002", "완료된 여행만 후기를 작성할 수 있습니다."),
	REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW_404_001", "후기를 찾을 수 없습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	ReviewErrorCode(HttpStatus httpStatus, String code, String message) {
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
