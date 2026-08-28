package com.withgahyo.domain.course.exception;

import com.withgahyo.global.exception.code.ErrorCode;
import org.springframework.http.HttpStatus;

public enum CourseErrorCode implements ErrorCode {
	INVALID_TRAVEL_PERIOD(HttpStatus.BAD_REQUEST, "COURSE_400_001", "여행 시작일은 오늘 이상이고 종료일은 시작일 이후여야 합니다."),
	REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "COURSE_404_001", "지역을 찾을 수 없습니다."),
	KEYWORD_NOT_FOUND(HttpStatus.NOT_FOUND, "COURSE_404_002", "관심 키워드를 찾을 수 없습니다."),
	PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "COURSE_404_003", "장소를 찾을 수 없습니다."),
	FAMILY_MEMBER_NOT_CONNECTED(HttpStatus.FORBIDDEN, "COURSE_403_001", "연결된 가족 구성원만 선택할 수 있습니다."),
	COURSE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "COURSE_403_002", "코스 접근 권한이 없습니다."),
	COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "COURSE_404_004", "코스를 찾을 수 없습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	CourseErrorCode(HttpStatus httpStatus, String code, String message) {
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
