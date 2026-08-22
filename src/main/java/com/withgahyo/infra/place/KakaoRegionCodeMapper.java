package com.withgahyo.infra.place;

final class KakaoRegionCodeMapper {

	private KakaoRegionCodeMapper() {
	}

	static String areaCodeFromAddress(String address) {
		if (address == null || address.isBlank()) {
			return null;
		}
		if (startsWithAny(address, "서울")) {
			return "1";
		}
		if (startsWithAny(address, "인천")) {
			return "2";
		}
		if (startsWithAny(address, "대전")) {
			return "3";
		}
		if (startsWithAny(address, "대구")) {
			return "4";
		}
		if (startsWithAny(address, "광주")) {
			return "5";
		}
		if (startsWithAny(address, "부산")) {
			return "6";
		}
		if (startsWithAny(address, "울산")) {
			return "7";
		}
		if (startsWithAny(address, "세종")) {
			return "8";
		}
		if (startsWithAny(address, "경기")) {
			return "31";
		}
		if (startsWithAny(address, "강원")) {
			return "32";
		}
		if (startsWithAny(address, "충북", "충청북도")) {
			return "33";
		}
		if (startsWithAny(address, "충남", "충청남도")) {
			return "34";
		}
		if (startsWithAny(address, "경북", "경상북도")) {
			return "35";
		}
		if (startsWithAny(address, "경남", "경상남도")) {
			return "36";
		}
		if (startsWithAny(address, "전북", "전라북도")) {
			return "37";
		}
		if (startsWithAny(address, "전남", "전라남도")) {
			return "38";
		}
		if (startsWithAny(address, "제주")) {
			return "39";
		}
		return null;
	}

	private static boolean startsWithAny(String address, String... prefixes) {
		for (String prefix : prefixes) {
			if (address.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}
}
