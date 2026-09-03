package com.withgahyo.domain.onboarding.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record FoodPreferenceSaveRequest(
	@NotNull List<Long> foodPreferenceIds
) {
}
