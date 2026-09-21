package com.withgahyo.infra.weather.dto;

import java.util.List;

/**
 * 기상청 단기예보 조회서비스(getVilageFcst) 원본 응답 구조. Frontend에는 절대 그대로 노출하지 않고
 * domain.weather 쪽에서 우리 서비스 DTO로 변환한다.
 */
public record KmaVilageFcstResponse(
	Response response
) {

	public record Response(
		Header header,
		Body body
	) {
	}

	public record Header(
		String resultCode,
		String resultMsg
	) {
		private static final String SUCCESS_CODE = "00";

		public boolean isSuccess() {
			return SUCCESS_CODE.equals(resultCode);
		}
	}

	public record Body(
		Items items
	) {
	}

	public record Items(
		List<Item> item
	) {
	}

	public record Item(
		String baseDate,
		String baseTime,
		String category,
		String fcstDate,
		String fcstTime,
		String fcstValue,
		Integer nx,
		Integer ny
	) {
	}
}
