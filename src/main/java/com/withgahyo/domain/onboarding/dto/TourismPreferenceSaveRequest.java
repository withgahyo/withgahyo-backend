package com.withgahyo.domain.onboarding.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record TourismPreferenceSaveRequest(
	@NotNull List<Long> tourismPreferenceIds
) {
}
