package com.withgahyo.domain.onboarding.dto;

import com.withgahyo.domain.user.entity.UserOnboardingProfile;
import java.util.List;

public record OnboardingResponse(
	String tripDuration,
	String walkingTolerance,
	String restPreference,
	String stairsPreference,
	String slopePreference,
	String spicyPreference,
	List<Long> tourismPreferenceIds,
	List<Long> foodPreferenceIds,
	boolean onboardingCompleted
) {

	public static OnboardingResponse of(
		UserOnboardingProfile profile,
		List<Long> tourismPreferenceIds,
		List<Long> foodPreferenceIds
	) {
		if (profile == null) {
			return new OnboardingResponse(
				null, null, null, null, null, null,
				tourismPreferenceIds, foodPreferenceIds, false
			);
		}

		return new OnboardingResponse(
			profile.getTripDuration(),
			profile.getWalkingTolerance(),
			profile.getRestPreference(),
			profile.getStairsPreference(),
			profile.getSlopePreference(),
			profile.getSpicyPreference(),
			tourismPreferenceIds,
			foodPreferenceIds,
			profile.isOnboardingCompleted()
		);
	}
}
