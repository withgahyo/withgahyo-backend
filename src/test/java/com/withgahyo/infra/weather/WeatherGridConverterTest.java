package com.withgahyo.infra.weather;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * 기상청 LCC 변환식 구현을 알려진 국내 좌표 기준으로 검증한다(공식 문서/공개 자료에서 널리 인용되는
 * 격자 좌표와 대조). 외부 호출 없는 순수 함수라 독립적으로 테스트 가능하다.
 */
class WeatherGridConverterTest {

	@Test
	void toGrid_seoulCityHall() {
		WeatherGridConverter.Grid grid = WeatherGridConverter.toGrid(37.5665, 126.9780);

		assertThat(grid.nx()).isEqualTo(60);
		assertThat(grid.ny()).isEqualTo(127);
	}

	@Test
	void toGrid_busanCityHall() {
		WeatherGridConverter.Grid grid = WeatherGridConverter.toGrid(35.1796, 129.0756);

		assertThat(grid.nx()).isEqualTo(98);
		assertThat(grid.ny()).isEqualTo(76);
	}

	@Test
	void toGrid_daejeonCityHall() {
		WeatherGridConverter.Grid grid = WeatherGridConverter.toGrid(36.3504, 127.3845);

		assertThat(grid.nx()).isEqualTo(67);
		assertThat(grid.ny()).isEqualTo(100);
	}

	@Test
	void toGrid_jejuCity() {
		WeatherGridConverter.Grid grid = WeatherGridConverter.toGrid(33.4996, 126.5312);

		assertThat(grid.nx()).isEqualTo(53);
		assertThat(grid.ny()).isEqualTo(38);
	}

	@Test
	void toGrid_acceptsBigDecimal_sameAsDouble() {
		WeatherGridConverter.Grid fromBigDecimal = WeatherGridConverter.toGrid(
			new BigDecimal("37.5665"), new BigDecimal("126.9780")
		);
		WeatherGridConverter.Grid fromDouble = WeatherGridConverter.toGrid(37.5665, 126.9780);

		assertThat(fromBigDecimal).isEqualTo(fromDouble);
	}

	@Test
	void toGrid_throws_whenLatitudeOrLongitudeIsNull() {
		org.assertj.core.api.Assertions.assertThatThrownBy(
			() -> WeatherGridConverter.toGrid(null, new BigDecimal("126.9780"))
		).isInstanceOf(IllegalArgumentException.class);
	}
}
