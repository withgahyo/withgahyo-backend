package com.withgahyo.domain.onboarding.dto;

import com.withgahyo.domain.family.entity.TourismPreference;

public record TourismPreferenceOptionResponse(
	Long id,
	String code,
	String name
) {

	public static TourismPreferenceOptionResponse from(TourismPreference tourismPreference) {
		return new TourismPreferenceOptionResponse(
			tourismPreference.getTourismPreferenceId(),
			tourismPreference.getCode(),
			tourismPreference.getName()
		);
	}
}
