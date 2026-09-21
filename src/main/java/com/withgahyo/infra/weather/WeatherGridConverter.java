package com.withgahyo.infra.weather;

import java.math.BigDecimal;

/**
 * 기상청 단기예보(getVilageFcst)가 요구하는 격자 좌표(nx, ny)를 위도/경도로부터 계산한다.
 * 기상청이 공개한 Lambert Conformal Conic(LCC) 투영 변환식을 그대로 구현한 순수 함수이며,
 * 지역별 nx/ny를 하드코딩한 매핑 테이블을 두지 않는다 — 좌표만 있으면 전국 어디든 계산 가능하다.
 * 외부 호출이 없어 단위 테스트만으로 검증할 수 있다.
 */
public final class WeatherGridConverter {

	private static final double RE = 6371.00877; // 지구 반경(km)
	private static final double GRID = 5.0; // 격자 간격(km)
	private static final double SLAT1 = 30.0; // 투영 위도1(degree)
	private static final double SLAT2 = 60.0; // 투영 위도2(degree)
	private static final double OLON = 126.0; // 기준점 경도(degree)
	private static final double OLAT = 38.0; // 기준점 위도(degree)
	private static final double XO = 43; // 기준점 X좌표(GRID)
	private static final double YO = 136; // 기준점 Y좌표(GRID)
	private static final double DEGRAD = Math.PI / 180.0;

	private WeatherGridConverter() {
	}

	public record Grid(int nx, int ny) {
	}

	public static Grid toGrid(BigDecimal latitude, BigDecimal longitude) {
		if (latitude == null || longitude == null) {
			throw new IllegalArgumentException("latitude/longitude는 null일 수 없습니다.");
		}
		return toGrid(latitude.doubleValue(), longitude.doubleValue());
	}

	public static Grid toGrid(double latitude, double longitude) {
		double re = RE / GRID;
		double slat1 = SLAT1 * DEGRAD;
		double slat2 = SLAT2 * DEGRAD;
		double olon = OLON * DEGRAD;
		double olat = OLAT * DEGRAD;

		double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
		sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
		double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
		sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;
		double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
		ro = re * sf / Math.pow(ro, sn);

		double ra = Math.tan(Math.PI * 0.25 + latitude * DEGRAD * 0.5);
		ra = re * sf / Math.pow(ra, sn);
		double theta = longitude * DEGRAD - olon;
		if (theta > Math.PI) {
			theta -= 2.0 * Math.PI;
		}
		if (theta < -Math.PI) {
			theta += 2.0 * Math.PI;
		}
		theta *= sn;

		int nx = (int) Math.floor(ra * Math.sin(theta) + XO + 0.5);
		int ny = (int) Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);
		return new Grid(nx, ny);
	}
}
