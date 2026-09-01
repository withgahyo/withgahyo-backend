package com.withgahyo.domain.recommendation.exception;

import com.withgahyo.global.exception.code.ErrorCode;
import org.springframework.http.HttpStatus;

public enum RecommendationErrorCode implements ErrorCode {
	RECOMMENDATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "RECOMMENDATION_403_001", "추천 생성 작업 접근 권한이 없습니다."),
	RECOMMENDATION_JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "RECOMMENDATION_404_001", "추천 생성 작업을 찾을 수 없습니다."),
	RECOMMENDATION_CANDIDATE_NOT_FOUND(HttpStatus.NOT_FOUND, "RECOMMENDATION_404_002", "추천 후보를 찾을 수 없습니다."),
	RECOMMENDATION_JOB_NOT_COMPLETED(HttpStatus.CONFLICT, "RECOMMENDATION_409_001", "추천 생성이 완료된 후 후보를 조회할 수 있습니다."),
	RECOMMENDATION_CANDIDATE_ALREADY_SELECTED(HttpStatus.CONFLICT, "RECOMMENDATION_409_002", "이미 다른 추천 후보가 확정되었습니다."),
	COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "RECOMMENDATION_404_003", "코스를 찾을 수 없습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	RecommendationErrorCode(HttpStatus httpStatus, String code, String message) {
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
