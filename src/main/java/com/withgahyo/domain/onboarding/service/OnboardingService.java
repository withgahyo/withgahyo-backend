package com.withgahyo.domain.onboarding.service;

import com.withgahyo.domain.family.entity.FoodPreference;
import com.withgahyo.domain.family.entity.TourismPreference;
import com.withgahyo.domain.family.repository.FoodPreferenceRepository;
import com.withgahyo.domain.family.repository.TourismPreferenceRepository;
import com.withgahyo.domain.onboarding.dto.ConditionsRequest;
import com.withgahyo.domain.onboarding.dto.FoodPreferenceOptionResponse;
import com.withgahyo.domain.onboarding.dto.FoodPreferenceSaveRequest;
import com.withgahyo.domain.onboarding.dto.OnboardingResponse;
import com.withgahyo.domain.onboarding.dto.OnboardingSaveRequest;
import com.withgahyo.domain.onboarding.dto.TourismPreferenceOptionResponse;
import com.withgahyo.domain.onboarding.dto.TourismPreferenceSaveRequest;
import com.withgahyo.domain.onboarding.dto.TripDurationRequest;
import com.withgahyo.domain.onboarding.exception.OnboardingErrorCode;
import com.withgahyo.domain.user.entity.User;
import com.withgahyo.domain.user.entity.UserFoodPreference;
import com.withgahyo.domain.user.entity.UserOnboardingProfile;
import com.withgahyo.domain.user.entity.UserTourismPreference;
import com.withgahyo.domain.user.repository.UserFoodPreferenceRepository;
import com.withgahyo.domain.user.repository.UserOnboardingProfileRepository;
import com.withgahyo.domain.user.repository.UserRepository;
import com.withgahyo.domain.user.repository.UserTourismPreferenceRepository;
import com.withgahyo.global.exception.BusinessException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OnboardingService {

	private final UserRepository userRepository;
	private final UserOnboardingProfileRepository userOnboardingProfileRepository;
	private final UserTourismPreferenceRepository userTourismPreferenceRepository;
	private final UserFoodPreferenceRepository userFoodPreferenceRepository;
	private final TourismPreferenceRepository tourismPreferenceRepository;
	private final FoodPreferenceRepository foodPreferenceRepository;

	public OnboardingResponse getOnboarding(Long userId) {
		UserOnboardingProfile profile = userOnboardingProfileRepository.findById(userId).orElse(null);
		List<Long> tourismPreferenceIds = findTourismPreferenceIds(userId);
		List<Long> foodPreferenceIds = findFoodPreferenceIds(userId);
		return OnboardingResponse.of(profile, tourismPreferenceIds, foodPreferenceIds);
	}

	@Transactional
	public void saveOnboarding(Long userId, OnboardingSaveRequest request) {
		UserOnboardingProfile profile = getOrCreateProfile(userId);
		profile.updateProfile(
			request.tripDuration(),
			request.walkingTolerance(),
			request.restPreference(),
			request.stairsPreference(),
			request.slopePreference(),
			request.spicyPreference()
		);
		replaceTourismPreferences(userId, request.tourismPreferenceIds());
		replaceFoodPreferences(userId, request.foodPreferenceIds());
	}

	public List<TourismPreferenceOptionResponse> getTourismPreferenceOptions() {
		return tourismPreferenceRepository.findAllByActiveTrueOrderByTourismPreferenceIdAsc().stream()
			.map(TourismPreferenceOptionResponse::from)
			.toList();
	}

	public List<FoodPreferenceOptionResponse> getFoodPreferenceOptions() {
		return foodPreferenceRepository.findAllByActiveTrueOrderByFoodPreferenceIdAsc().stream()
			.map(FoodPreferenceOptionResponse::from)
			.toList();
	}

	@Transactional
	public void saveTripDuration(Long userId, TripDurationRequest request) {
		UserOnboardingProfile profile = getOrCreateProfile(userId);
		profile.updateTripDuration(request.tripDuration());
	}

	@Transactional
	public void saveTourismPreferences(Long userId, TourismPreferenceSaveRequest request) {
		replaceTourismPreferences(userId, request.tourismPreferenceIds());
	}

	@Transactional
	public void saveFoodPreferences(Long userId, FoodPreferenceSaveRequest request) {
		replaceFoodPreferences(userId, request.foodPreferenceIds());
	}

	@Transactional
	public void saveConditions(Long userId, ConditionsRequest request) {
		UserOnboardingProfile profile = getOrCreateProfile(userId);
		profile.updateConditions(
			request.walkingTolerance(),
			request.restPreference(),
			request.stairsPreference(),
			request.slopePreference(),
			request.spicyPreference()
		);
	}

	@Transactional
	public void completeOnboarding(Long userId) {
		UserOnboardingProfile profile = userOnboardingProfileRepository.findById(userId).orElse(null);
		if (profile != null && profile.isOnboardingCompleted()) {
			return;
		}

		boolean hasTourismPreference = userTourismPreferenceRepository.existsById_UserId(userId);
		boolean hasFoodPreference = userFoodPreferenceRepository.existsById_UserId(userId);
		if (profile == null || !profile.isProfileFilled() || !hasTourismPreference || !hasFoodPreference) {
			throw new BusinessException(OnboardingErrorCode.INCOMPLETE_ONBOARDING);
		}

		profile.complete();
	}

	private List<Long> findTourismPreferenceIds(Long userId) {
		return userTourismPreferenceRepository.findAllById_UserId(userId).stream()
			.map(preference -> preference.getTourismPreference().getTourismPreferenceId())
			.toList();
	}

	private List<Long> findFoodPreferenceIds(Long userId) {
		return userFoodPreferenceRepository.findAllById_UserId(userId).stream()
			.map(preference -> preference.getFoodPreference().getFoodPreferenceId())
			.toList();
	}

	private UserOnboardingProfile getOrCreateProfile(Long userId) {
		return userOnboardingProfileRepository.findById(userId)
			.orElseGet(() -> {
				User user = userRepository.getReferenceById(userId);
				return userOnboardingProfileRepository.save(UserOnboardingProfile.create(user));
			});
	}

	private void replaceTourismPreferences(Long userId, List<Long> tourismPreferenceIds) {
		List<TourismPreference> preferences = tourismPreferenceRepository.findAllById(tourismPreferenceIds);
		Set<Long> requestedIds = new HashSet<>(tourismPreferenceIds);
		if (preferences.size() != requestedIds.size()) {
			throw new BusinessException(OnboardingErrorCode.INVALID_TOURISM_PREFERENCE);
		}

		userTourismPreferenceRepository.deleteAllById_UserId(userId);
		User user = userRepository.getReferenceById(userId);
		List<UserTourismPreference> mappings = preferences.stream()
			.map(preference -> UserTourismPreference.create(user, preference))
			.toList();
		userTourismPreferenceRepository.saveAll(mappings);
	}

	private void replaceFoodPreferences(Long userId, List<Long> foodPreferenceIds) {
		List<FoodPreference> preferences = foodPreferenceRepository.findAllById(foodPreferenceIds);
		Set<Long> requestedIds = new HashSet<>(foodPreferenceIds);
		if (preferences.size() != requestedIds.size()) {
			throw new BusinessException(OnboardingErrorCode.INVALID_FOOD_PREFERENCE);
		}

		userFoodPreferenceRepository.deleteAllById_UserId(userId);
		User user = userRepository.getReferenceById(userId);
		List<UserFoodPreference> mappings = preferences.stream()
			.map(preference -> UserFoodPreference.create(user, preference))
			.toList();
		userFoodPreferenceRepository.saveAll(mappings);
	}
}
