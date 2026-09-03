package com.withgahyo.domain.onboarding.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.withgahyo.domain.onboarding.dto.ConditionsRequest;
import com.withgahyo.domain.onboarding.dto.OnboardingResponse;
import com.withgahyo.domain.onboarding.dto.OnboardingSaveRequest;
import com.withgahyo.domain.onboarding.dto.TourismPreferenceSaveRequest;
import com.withgahyo.domain.onboarding.service.OnboardingService;
import com.withgahyo.global.exception.BusinessException;
import com.withgahyo.global.exception.code.SecurityErrorCode;
import com.withgahyo.global.security.AuthenticatedUser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OnboardingControllerTest {

	private static final AuthenticatedUser AUTHENTICATED_USER = new AuthenticatedUser(1L);

	@Mock
	private OnboardingService onboardingService;

	private OnboardingController onboardingController;

	private OnboardingController controller() {
		return new OnboardingController(onboardingService);
	}

	@Test
	void getOnboarding_delegatesWithUserId_fromAuthenticatedUser() {
		onboardingController = controller();
		OnboardingResponse response = new OnboardingResponse(
			null, null, null, null, null, List.of(), List.of(), false
		);
		given(onboardingService.getOnboarding(1L)).willReturn(response);

		var result = onboardingController.getOnboarding(AUTHENTICATED_USER);

		assertThat(result.data()).isEqualTo(response);
		verify(onboardingService).getOnboarding(1L);
	}

	@Test
	void saveOnboarding_delegatesWithUserId_fromAuthenticatedUser() {
		onboardingController = controller();
		OnboardingSaveRequest request = new OnboardingSaveRequest(
			null, null, null, null, null, List.of(10L), List.of(20L)
		);

		onboardingController.saveOnboarding(AUTHENTICATED_USER, request);

		verify(onboardingService).saveOnboarding(1L, request);
	}

	@Test
	void saveTourismPreferences_delegatesWithUserId_fromAuthenticatedUser() {
		onboardingController = controller();
		TourismPreferenceSaveRequest request = new TourismPreferenceSaveRequest(List.of(10L));

		onboardingController.saveTourismPreferences(AUTHENTICATED_USER, request);

		verify(onboardingService).saveTourismPreferences(1L, request);
	}

	@Test
	void saveConditions_delegatesWithUserId_fromAuthenticatedUser() {
		onboardingController = controller();
		ConditionsRequest request = new ConditionsRequest(null, null, null, null, null);

		onboardingController.saveConditions(AUTHENTICATED_USER, request);

		verify(onboardingService).saveConditions(1L, request);
	}

	@Test
	void completeOnboarding_delegatesWithUserId_fromAuthenticatedUser() {
		onboardingController = controller();

		onboardingController.completeOnboarding(AUTHENTICATED_USER);

		verify(onboardingService).completeOnboarding(1L);
	}

	@Test
	void getOnboarding_throwsUnauthorized_whenAuthenticatedUserIsNull() {
		onboardingController = controller();

		assertThatThrownBy(() -> onboardingController.getOnboarding(null))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
				.isEqualTo(SecurityErrorCode.UNAUTHORIZED));
	}
}
