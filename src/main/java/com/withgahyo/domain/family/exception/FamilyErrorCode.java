package com.withgahyo.domain.family.exception;

import com.withgahyo.global.exception.code.ErrorCode;
import org.springframework.http.HttpStatus;

public enum FamilyErrorCode implements ErrorCode {
	SELF_CONNECTION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "FAMILY_400_001", "본인은 가족 구성원으로 연결할 수 없습니다."),
	FAMILY_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "FAMILY_404_001", "가입된 가족 구성원을 찾을 수 없습니다."),
	ALREADY_CONNECTED(HttpStatus.CONFLICT, "FAMILY_409_001", "이미 연결된 가족 구성원입니다."),
	EMAIL_MATCHES_MULTIPLE_USERS(HttpStatus.CONFLICT, "FAMILY_409_002", "해당 이메일에 연결된 계정이 여러 개입니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	FamilyErrorCode(HttpStatus httpStatus, String code, String message) {
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
