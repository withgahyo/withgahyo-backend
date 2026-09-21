package com.withgahyo.domain.weather.dto;

/**
 * 기상청 raw SKY/PTY 코드를 그대로 노출하지 않기 위한 서비스용 날씨 상태. Frontend가 아이콘을
 * 바로 결정할 수 있는 최소 단위로만 나누고 더 세분화하지 않는다.
 */
public enum WeatherCondition {
	SUNNY,
	CLOUDY,
	RAIN,
	SNOW,
	RAIN_SNOW
}
