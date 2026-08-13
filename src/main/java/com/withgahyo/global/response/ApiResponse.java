package com.withgahyo.global.response;

public record ApiResponse<T>(
	boolean success,
	String code,
	String message,
	T data
) {

	private static final String SUCCESS_CODE = "COMMON_SUCCESS";
	private static final String SUCCESS_MESSAGE = "요청이 성공했습니다.";

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(true, SUCCESS_CODE, SUCCESS_MESSAGE, data);
	}

	// record의 success 필드가 success() 접근자를 생성하므로, 데이터 없는 성공 응답은 ok()로 구분
	public static ApiResponse<Void> ok() {
		return new ApiResponse<>(true, SUCCESS_CODE, SUCCESS_MESSAGE, null);
	}
}
