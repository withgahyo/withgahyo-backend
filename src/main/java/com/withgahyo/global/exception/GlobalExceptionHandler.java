package com.withgahyo.global.exception;

import com.withgahyo.global.exception.code.CommonErrorCode;
import com.withgahyo.global.exception.code.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessException(
		BusinessException exception,
		HttpServletRequest request
	) {
		return handle(exception.getErrorCode(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
		MethodArgumentNotValidException exception,
		HttpServletRequest request
	) {
		List<ErrorResponse.FieldError> errors = exception.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
			.toList();

		return handle(CommonErrorCode.INVALID_INPUT_VALUE, request, errors);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolation(
		ConstraintViolationException exception,
		HttpServletRequest request
	) {
		List<ErrorResponse.FieldError> errors = exception.getConstraintViolations()
			.stream()
			.map(violation -> new ErrorResponse.FieldError(
				violation.getPropertyPath().toString(),
				violation.getMessage()))
			.toList();

		return handle(CommonErrorCode.INVALID_INPUT_VALUE, request, errors);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(HttpServletRequest request) {
		return handle(CommonErrorCode.METHOD_ARGUMENT_TYPE_MISMATCH, request);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpServletRequest request) {
		return handle(CommonErrorCode.METHOD_NOT_ALLOWED, request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(HttpServletRequest request) {
		return handle(CommonErrorCode.INTERNAL_SERVER_ERROR, request);
	}

	private ResponseEntity<ErrorResponse> handle(ErrorCode errorCode, HttpServletRequest request) {
		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ErrorResponse.of(errorCode, request.getRequestURI()));
	}

	private ResponseEntity<ErrorResponse> handle(
		ErrorCode errorCode,
		HttpServletRequest request,
		List<ErrorResponse.FieldError> errors
	) {
		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ErrorResponse.of(errorCode, request.getRequestURI(), errors));
	}
}
