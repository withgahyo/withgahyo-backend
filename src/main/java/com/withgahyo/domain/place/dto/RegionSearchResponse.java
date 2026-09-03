package com.withgahyo.domain.place.dto;

import com.withgahyo.domain.place.entity.Region;
import java.util.List;

public record RegionSearchResponse(List<RegionResponse> regions) {

	public static RegionSearchResponse from(List<Region> regions) {
		return new RegionSearchResponse(regions.stream().map(RegionResponse::from).toList());
	}

	public record RegionResponse(
		String areaCode,
		String sigunguCode,
		String name,
		String parentName,
		String displayName
	) {
		private static RegionResponse from(Region region) {
			String parentName = RegionNameFormatter.parentName(region.getName());
			return new RegionResponse(
				region.getAreaCode(),
				region.getSigunguCode(),
				region.getName(),
				parentName,
				RegionNameFormatter.displayName(parentName, region.getName())
			);
		}
	}
}
