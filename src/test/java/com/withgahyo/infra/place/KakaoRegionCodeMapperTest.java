package com.withgahyo.infra.place;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KakaoRegionCodeMapperTest {

	@Test
	void areaCodeFromAddress_mapsKakaoAddressToTourApiAreaCode() {
		assertThat(KakaoRegionCodeMapper.areaCodeFromAddress("서울특별시 성동구 뚝섬로 273")).isEqualTo("1");
		assertThat(KakaoRegionCodeMapper.areaCodeFromAddress("대전광역시 서구 둔산대로 169")).isEqualTo("3");
		assertThat(KakaoRegionCodeMapper.areaCodeFromAddress("제주특별자치도 제주시 첨단로 242")).isEqualTo("39");
	}
}
