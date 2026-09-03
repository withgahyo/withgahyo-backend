package com.withgahyo.domain.place.dto;

final class RegionNameFormatter {

	private RegionNameFormatter() {
	}

	static String parentName(String name) {
		return name.split(" ")[0];
	}

	static String displayName(String parentName, String name) {
		String[] parts = name.split(" ");
		if (parts.length < 2) {
			return abbreviate(parentName);
		}
		return abbreviate(parentName) + " " + parts[parts.length - 1];
	}

	private static String abbreviate(String parentName) {
		return parentName
			.replace("특별시", "")
			.replace("광역시", "")
			.replace("특별자치시", "")
			.replace("특별자치도", "")
			.replace("충청북도", "충북")
			.replace("충청남도", "충남")
			.replace("전라북도", "전북")
			.replace("전라남도", "전남")
			.replace("경상북도", "경북")
			.replace("경상남도", "경남")
			.replace("도", "");
	}
}
