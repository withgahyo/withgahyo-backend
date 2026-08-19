package com.withgahyo.domain.onboarding.dto;

import com.withgahyo.domain.family.entity.FoodPreference;

public record FoodPreferenceOptionResponse(
	Long id,
	String code,
	String name
) {

	public static FoodPreferenceOptionResponse from(FoodPreference foodPreference) {
		return new FoodPreferenceOptionResponse(
			foodPreference.getFoodPreferenceId(),
			foodPreference.getCode(),
			foodPreference.getName()
		);
	}
}
