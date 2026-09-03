package com.withgahyo.domain.place.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RegionNameFormatterTest {

	@Test
	void displayName_abbreviatesMetropolitanCity() {
		String parentName = RegionNameFormatter.parentName("대전광역시 동구");

		assertThat(RegionNameFormatter.displayName(parentName, "대전광역시 동구")).isEqualTo("대전 동구");
	}

	@Test
	void displayName_abbreviatesCompoundProvinces() {
		assertThat(displayNameOf("충청북도 청주시")).isEqualTo("충북 청주시");
		assertThat(displayNameOf("충청남도 천안시")).isEqualTo("충남 천안시");
		assertThat(displayNameOf("전라북도 전주시")).isEqualTo("전북 전주시");
		assertThat(displayNameOf("전라남도 여수시")).isEqualTo("전남 여수시");
		assertThat(displayNameOf("경상북도 포항시")).isEqualTo("경북 포항시");
		assertThat(displayNameOf("경상남도 창원시")).isEqualTo("경남 창원시");
	}

	private String displayNameOf(String name) {
		String parentName = RegionNameFormatter.parentName(name);
		return RegionNameFormatter.displayName(parentName, name);
	}
}
