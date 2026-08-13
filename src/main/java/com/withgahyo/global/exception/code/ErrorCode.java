package com.withgahyo.global.exception.code;

import org.springframework.http.HttpStatus;

/**
 * 에러 코드 정의 컨벤션
 *
 * - 형식: {도메인}_{HTTP상태}_{일련번호} (예: COMMON_400_001)
 * - 순번 기준: 등록 순서대로 채번
 * - 번호 범위: 도메인 + HTTP 상태 조합별로 독립적으로 관리 (COMMON_400과 AUTH_401은 서로 별개)
 * - 삭제된 번호는 재사용하지 않음 (다음 신규 코드는 항상 다음 번호로)
 * - 도메인 prefix는 대문자 단수형 사용 (예: MEMBER, COURSE, PLACE - MEMBERS, COURSES 금지)
 */
public interface ErrorCode {

	HttpStatus getHttpStatus();

	String getCode();

	String getMessage();
}
