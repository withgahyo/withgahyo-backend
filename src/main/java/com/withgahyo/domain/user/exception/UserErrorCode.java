package com.withgahyo.domain.user.exception;

import com.withgahyo.global.exception.code.ErrorCode;
import org.springframework.http.HttpStatus;

public enum UserErrorCode implements ErrorCode {

	USER_UPDATE_EMPTY(HttpStatus.BAD_REQUEST, "USER_400_001", "수정할 프로필 정보가 없습니다."),
	INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "USER_400_002", "닉네임은 2자 이상 20자 이하여야 합니다."),
	INVALID_PROFILE_IMAGE(HttpStatus.BAD_REQUEST, "USER_400_003", "프로필 이미지는 jpg, png, webp 형식만 업로드할 수 있습니다."),
	PROFILE_IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "USER_500_001", "프로필 이미지 업로드에 실패했습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	UserErrorCode(HttpStatus httpStatus, String code, String message) {
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
