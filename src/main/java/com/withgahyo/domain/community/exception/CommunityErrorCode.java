package com.withgahyo.domain.community.exception;

import com.withgahyo.global.exception.code.ErrorCode;
import org.springframework.http.HttpStatus;

public enum CommunityErrorCode implements ErrorCode {
	POST_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMUNITY_404_001", "커뮤니티 게시글을 찾을 수 없습니다."),
	SELF_BLOCK_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "COMMUNITY_400_001", "자기 자신은 차단할 수 없습니다."),
	POST_ALREADY_SHARED(HttpStatus.CONFLICT, "COMMUNITY_409_001", "이미 커뮤니티에 공유한 후기입니다."),
	POST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "COMMUNITY_403_001", "본인이 작성한 게시글만 처리할 수 있습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	CommunityErrorCode(HttpStatus httpStatus, String code, String message) {
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
