package com.withgahyo.domain.place.dto;

final class RegionNameFormatter {

	private RegionNameFormatter() {
	}

	static String parentName(String name) {
		String[] parts = name.split(" ");
		return parts.length == 0 ? name : parts[0];
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
			.replace("도", "");
	}
}
