package com.withgahyo.global.exception;

import com.withgahyo.global.exception.code.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
	boolean success,
	LocalDateTime timestamp,
	int status,
	String code,
	String message,
	String path,
	List<FieldError> errors
) {

	public static ErrorResponse of(ErrorCode errorCode, String path) {
		return new ErrorResponse(
			false,
			LocalDateTime.now(),
			errorCode.getHttpStatus().value(),
			errorCode.getCode(),
			errorCode.getMessage(),
			path,
			List.of());
	}

	public static ErrorResponse of(ErrorCode errorCode, String path, List<FieldError> errors) {
		return new ErrorResponse(
			false,
			LocalDateTime.now(),
			errorCode.getHttpStatus().value(),
			errorCode.getCode(),
			errorCode.getMessage(),
			path,
			errors);
	}

	public record FieldError(String field, String reason) {
	}
}
