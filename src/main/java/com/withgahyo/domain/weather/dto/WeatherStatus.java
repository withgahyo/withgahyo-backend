package com.withgahyo.domain.weather.dto;

/**
 * Frontend가 boolean 하나로는 표현하기 부족한 "다가오는 여행 날씨" 상태를 분기하기 위한 최소 상태 집합.
 * 이 이상으로 세분화하지 않는다.
 */
public enum WeatherStatus {
	AVAILABLE, // 여행 전체 기간이 예보 범위 안 — 정상 조회
	PARTIALLY_AVAILABLE, // 여행 일부 날짜만 예보 범위 안
	OUT_OF_FORECAST_RANGE, // 코스/위치는 정상이지만 여행 전체가 단기예보 범위 밖
	NO_UPCOMING_TRIP, // 예정 여행 없음
	LOCATION_UNAVAILABLE, // 확정 일정/장소/좌표를 찾을 수 없어 위치를 결정할 수 없음
	EXTERNAL_API_ERROR // 기상청 API 호출 실패(타임아웃, 4xx/5xx, error resultCode 등)
}
