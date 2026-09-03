package com.withgahyo.domain.onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import com.withgahyo.domain.onboarding.exception.OnboardingErrorCode;
import com.withgahyo.domain.user.entity.User;
import com.withgahyo.domain.user.entity.UserOnboardingProfile;
import com.withgahyo.domain.user.repository.UserFoodPreferenceRepository;
import com.withgahyo.domain.user.repository.UserOnboardingProfileRepository;
import com.withgahyo.domain.user.repository.UserRepository;
import com.withgahyo.domain.user.repository.UserTourismPreferenceRepository;
import com.withgahyo.global.exception.BusinessException;
import jakarta.persistence.EntityManager;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private UserOnboardingProfileRepository userOnboardingProfileRepository;
	@Mock
	private UserTourismPreferenceRepository userTourismPreferenceRepository;
	@Mock
	private UserFoodPreferenceRepository userFoodPreferenceRepository;
	@Mock
	private TourismPreferenceRepository tourismPreferenceRepository;
	@Mock
	private FoodPreferenceRepository foodPreferenceRepository;
	@Mock
	private EntityManager entityManager;

	@InjectMocks
	private OnboardingService onboardingService;

	private static final Long USER_ID = 1L;

	@Test
	void getOnboarding_returnsDefaultResponse_whenProfileNotExists() {
		given(userOnboardingProfileRepository.findById(USER_ID)).willReturn(Optional.empty());
		given(userTourismPreferenceRepository.findAllById_UserId(USER_ID)).willReturn(List.of());
		given(userFoodPreferenceRepository.findAllById_UserId(USER_ID)).willReturn(List.of());

		OnboardingResponse response = onboardingService.getOnboarding(USER_ID);

		assertThat(response.walkingTolerance()).isNull();
		assertThat(response.tourismPreferenceIds()).isEmpty();
		assertThat(response.foodPreferenceIds()).isEmpty();
		assertThat(response.onboardingCompleted()).isFalse();
	}

	@Test
	void saveTourismPreferences_throwsException_whenIdIsInvalid() {
		given(tourismPreferenceRepository.findAllById(anyList())).willReturn(List.of());

		assertThatThrownBy(() -> onboardingService.saveTourismPreferences(
			USER_ID, new TourismPreferenceSaveRequest(List.of(999L))
		))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
				.isEqualTo(OnboardingErrorCode.INVALID_TOURISM_PREFERENCE));

		verify(userTourismPreferenceRepository, never()).deleteAllById_UserId(anyLong());
	}

	@Test
	void saveFoodPreferences_throwsException_whenIdIsInvalid() {
		given(foodPreferenceRepository.findAllById(anyList())).willReturn(List.of());

		assertThatThrownBy(() -> onboardingService.saveFoodPreferences(
			USER_ID, new FoodPreferenceSaveRequest(List.of(999L))
		))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
				.isEqualTo(OnboardingErrorCode.INVALID_FOOD_PREFERENCE));

		verify(userFoodPreferenceRepository, never()).deleteAllById_UserId(anyLong());
	}

	@Test
	void saveTourismPreferences_replacesMappings_whenIdsAreValid() {
		TourismPreference preference = tourismPreference(10L);
		given(tourismPreferenceRepository.findAllById(anyList())).willReturn(List.of(preference));
		given(userRepository.getReferenceById(USER_ID)).willReturn(user());

		onboardingService.saveTourismPreferences(USER_ID, new TourismPreferenceSaveRequest(List.of(10L)));

		verify(userTourismPreferenceRepository).deleteAllById_UserId(USER_ID);
		verify(userTourismPreferenceRepository).saveAll(anyList());
	}

	@Test
	void saveFoodPreferences_replacesMappings_whenIdsAreValid() {
		FoodPreference preference = foodPreference(20L);
		given(foodPreferenceRepository.findAllById(anyList())).willReturn(List.of(preference));
		given(userRepository.getReferenceById(USER_ID)).willReturn(user());

		onboardingService.saveFoodPreferences(USER_ID, new FoodPreferenceSaveRequest(List.of(20L)));

		verify(userFoodPreferenceRepository).deleteAllById_UserId(USER_ID);
		verify(userFoodPreferenceRepository).saveAll(anyList());
	}

	@Test
	void saveConditions_success_updatesConditionFields() {
		UserOnboardingProfile profile = UserOnboardingProfile.create(user());
		given(userOnboardingProfileRepository.findById(USER_ID)).willReturn(Optional.of(profile));

		onboardingService.saveConditions(USER_ID, new ConditionsRequest(
			"많이 걸어도 됨", "자주 쉼", "가능", "가능", "매운맛 선호"
		));

		assertThat(profile.getWalkingTolerance()).isEqualTo("많이 걸어도 됨");
		assertThat(profile.getRestPreference()).isEqualTo("자주 쉼");
		assertThat(profile.getStairsPreference()).isEqualTo("가능");
		assertThat(profile.getSlopePreference()).isEqualTo("가능");
		assertThat(profile.getSpicyPreference()).isEqualTo("매운맛 선호");
	}

	@Test
	void saveConditions_success_whenSlopePreferenceIsNull() {
		UserOnboardingProfile profile = UserOnboardingProfile.create(user());
		given(userOnboardingProfileRepository.findById(USER_ID)).willReturn(Optional.of(profile));

		onboardingService.saveConditions(USER_ID, new ConditionsRequest(
			"많이 걸어도 됨", "자주 쉼", "가능", null, "매운맛 선호"
		));

		assertThat(profile.getSlopePreference()).isNull();
		assertThat(profile.getWalkingTolerance()).isEqualTo("많이 걸어도 됨");
	}

	@Test
	void saveConditions_success_whenAllConditionsAreNull() {
		UserOnboardingProfile profile = UserOnboardingProfile.create(user());
		given(userOnboardingProfileRepository.findById(USER_ID)).willReturn(Optional.of(profile));

		onboardingService.saveConditions(USER_ID, new ConditionsRequest(null, null, null, null, null));

		assertThat(profile.getWalkingTolerance()).isNull();
		assertThat(profile.getRestPreference()).isNull();
		assertThat(profile.getStairsPreference()).isNull();
		assertThat(profile.getSlopePreference()).isNull();
		assertThat(profile.getSpicyPreference()).isNull();
	}

	@Test
	void saveOnboarding_success_updatesProfileAndReplacesBothPreferences() {
		UserOnboardingProfile profile = UserOnboardingProfile.create(user());
		given(userOnboardingProfileRepository.findById(USER_ID)).willReturn(Optional.of(profile));

		TourismPreference tourismPreference = tourismPreference(10L);
		FoodPreference foodPreference = foodPreference(20L);
		given(tourismPreferenceRepository.findAllById(anyList())).willReturn(List.of(tourismPreference));
		given(foodPreferenceRepository.findAllById(anyList())).willReturn(List.of(foodPreference));
		given(userRepository.getReferenceById(USER_ID)).willReturn(user());

		OnboardingSaveRequest request = new OnboardingSaveRequest(
			"많이 걸어도 됨", "자주 쉼", "가능", "가능", "매운맛 선호",
			List.of(10L), List.of(20L)
		);

		onboardingService.saveOnboarding(USER_ID, request);

		assertThat(profile.getWalkingTolerance()).isEqualTo("많이 걸어도 됨");
		assertThat(profile.getRestPreference()).isEqualTo("자주 쉼");
		assertThat(profile.getStairsPreference()).isEqualTo("가능");
		assertThat(profile.getSlopePreference()).isEqualTo("가능");
		assertThat(profile.getSpicyPreference()).isEqualTo("매운맛 선호");

		verify(userTourismPreferenceRepository).deleteAllById_UserId(USER_ID);
		verify(userTourismPreferenceRepository).saveAll(anyList());
		verify(userFoodPreferenceRepository).deleteAllById_UserId(USER_ID);
		verify(userFoodPreferenceRepository).saveAll(anyList());
	}

	@Test
	void saveOnboarding_success_whenAllConditionsAreNull() {
		UserOnboardingProfile profile = UserOnboardingProfile.create(user());
		given(userOnboardingProfileRepository.findById(USER_ID)).willReturn(Optional.of(profile));

		TourismPreference tourismPreference = tourismPreference(10L);
		FoodPreference foodPreference = foodPreference(20L);
		given(tourismPreferenceRepository.findAllById(anyList())).willReturn(List.of(tourismPreference));
		given(foodPreferenceRepository.findAllById(anyList())).willReturn(List.of(foodPreference));
		given(userRepository.getReferenceById(USER_ID)).willReturn(user());

		OnboardingSaveRequest request = new OnboardingSaveRequest(
			null, null, null, null, null,
			List.of(10L), List.of(20L)
		);

		onboardingService.saveOnboarding(USER_ID, request);

		assertThat(profile.getWalkingTolerance()).isNull();
		assertThat(profile.getRestPreference()).isNull();
		assertThat(profile.getStairsPreference()).isNull();
		assertThat(profile.getSlopePreference()).isNull();
		assertThat(profile.getSpicyPreference()).isNull();
	}

	@Test
	void getTourismPreferenceOptions_returnsOnlyActivePreferences() {
		TourismPreference preference1 = tourismPreference(1L, "NATURE", "자연 관광");
		TourismPreference preference2 = tourismPreference(2L, "CULTURE", "문화 체험");
		given(tourismPreferenceRepository.findAllByActiveTrueOrderByTourismPreferenceIdAsc())
			.willReturn(List.of(preference1, preference2));

		List<TourismPreferenceOptionResponse> result = onboardingService.getTourismPreferenceOptions();

		assertThat(result).hasSize(2);
		assertThat(result.get(0).id()).isEqualTo(1L);
		assertThat(result.get(0).code()).isEqualTo("NATURE");
		assertThat(result.get(0).name()).isEqualTo("자연 관광");
		assertThat(result.get(1).id()).isEqualTo(2L);

		// 비활성 취향은 findAllByActiveTrueOrderBy... 쿼리 단계에서 이미 제외되므로,
		// Service가 findAll()이 아닌 이 필터링된 쿼리를 사용하는지까지 함께 검증한다.
		verify(tourismPreferenceRepository).findAllByActiveTrueOrderByTourismPreferenceIdAsc();
		verify(tourismPreferenceRepository, never()).findAll();
	}

	@Test
	void getFoodPreferenceOptions_returnsOnlyActivePreferences() {
		FoodPreference preference1 = foodPreference(1L, "KOREAN", "한식");
		FoodPreference preference2 = foodPreference(2L, "WESTERN", "양식");
		given(foodPreferenceRepository.findAllByActiveTrueOrderByFoodPreferenceIdAsc())
			.willReturn(List.of(preference1, preference2));

		List<FoodPreferenceOptionResponse> result = onboardingService.getFoodPreferenceOptions();

		assertThat(result).hasSize(2);
		assertThat(result.get(0).id()).isEqualTo(1L);
		assertThat(result.get(0).code()).isEqualTo("KOREAN");
		assertThat(result.get(0).name()).isEqualTo("한식");
		assertThat(result.get(1).id()).isEqualTo(2L);

		// 비활성 취향은 findAllByActiveTrueOrderBy... 쿼리 단계에서 이미 제외되므로,
		// Service가 findAll()이 아닌 이 필터링된 쿼리를 사용하는지까지 함께 검증한다.
		verify(foodPreferenceRepository).findAllByActiveTrueOrderByFoodPreferenceIdAsc();
		verify(foodPreferenceRepository, never()).findAll();
	}

	@Test
	void completeOnboarding_throwsException_whenTourismPreferenceMissing() {
		given(userOnboardingProfileRepository.findById(USER_ID)).willReturn(Optional.empty());
		given(userTourismPreferenceRepository.existsById_UserId(USER_ID)).willReturn(false);
		given(userFoodPreferenceRepository.existsById_UserId(USER_ID)).willReturn(true);

		assertThatThrownBy(() -> onboardingService.completeOnboarding(USER_ID))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
				.isEqualTo(OnboardingErrorCode.INCOMPLETE_ONBOARDING));
	}

	@Test
	void completeOnboarding_throwsException_whenFoodPreferenceMissing() {
		UserOnboardingProfile profile = UserOnboardingProfile.create(user());
		given(userOnboardingProfileRepository.findById(USER_ID)).willReturn(Optional.of(profile));
		given(userTourismPreferenceRepository.existsById_UserId(USER_ID)).willReturn(true);
		given(userFoodPreferenceRepository.existsById_UserId(USER_ID)).willReturn(false);

		assertThatThrownBy(() -> onboardingService.completeOnboarding(USER_ID))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
				.isEqualTo(OnboardingErrorCode.INCOMPLETE_ONBOARDING));
	}

	@Test
	void completeOnboarding_completes_whenTourismAndFoodPreferencesArePresent_regardlessOfConditions() {
		UserOnboardingProfile profile = UserOnboardingProfile.create(user());
		given(userOnboardingProfileRepository.findById(USER_ID)).willReturn(Optional.of(profile));
		given(userTourismPreferenceRepository.existsById_UserId(USER_ID)).willReturn(true);
		given(userFoodPreferenceRepository.existsById_UserId(USER_ID)).willReturn(true);

		onboardingService.completeOnboarding(USER_ID);

		assertThat(profile.isOnboardingCompleted()).isTrue();
	}

	@Test
	void completeOnboarding_createsProfile_whenProfileNotExists_butPreferencesArePresent() {
		given(userOnboardingProfileRepository.findById(USER_ID)).willReturn(Optional.empty());
		given(userTourismPreferenceRepository.existsById_UserId(USER_ID)).willReturn(true);
		given(userFoodPreferenceRepository.existsById_UserId(USER_ID)).willReturn(true);
		given(userRepository.getReferenceById(USER_ID)).willReturn(user());

		onboardingService.completeOnboarding(USER_ID);

		// 신규 프로필은 repository.save()가 아니라 EntityManager.persist()로 생성되어야 한다.
		ArgumentCaptor<UserOnboardingProfile> captor = ArgumentCaptor.forClass(UserOnboardingProfile.class);
		verify(entityManager).persist(captor.capture());
		verify(userOnboardingProfileRepository, never()).save(any(UserOnboardingProfile.class));
		assertThat(captor.getValue().isOnboardingCompleted()).isTrue();
	}

	@Test
	void completeOnboarding_isIdempotent_whenAlreadyCompleted() {
		UserOnboardingProfile profile = filledProfile();
		profile.complete();
		given(userOnboardingProfileRepository.findById(USER_ID)).willReturn(Optional.of(profile));

		onboardingService.completeOnboarding(USER_ID);

		assertThat(profile.isOnboardingCompleted()).isTrue();
		verify(userTourismPreferenceRepository, never()).existsById_UserId(anyLong());
	}

	private UserOnboardingProfile filledProfile() {
		UserOnboardingProfile profile = UserOnboardingProfile.create(user());
		profile.updateConditions("많이 걸어도 됨", "자주 쉼", "가능", "가능", "매운맛 선호");
		return profile;
	}

	private User user() {
		User user = newInstance(User.class);
		setField(user, "userId", USER_ID);
		return user;
	}

	private TourismPreference tourismPreference(Long id) {
		return tourismPreference(id, "CODE_" + id, "관광 취향 " + id);
	}

	private TourismPreference tourismPreference(Long id, String code, String name) {
		TourismPreference preference = newInstance(TourismPreference.class);
		setField(preference, "tourismPreferenceId", id);
		setField(preference, "code", code);
		setField(preference, "name", name);
		return preference;
	}

	private FoodPreference foodPreference(Long id) {
		return foodPreference(id, "CODE_" + id, "식사 취향 " + id);
	}

	private FoodPreference foodPreference(Long id, String code, String name) {
		FoodPreference preference = newInstance(FoodPreference.class);
		setField(preference, "foodPreferenceId", id);
		setField(preference, "code", code);
		setField(preference, "name", name);
		return preference;
	}

	private <T> T newInstance(Class<T> type) {
		try {
			Constructor<T> constructor = type.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	private void setField(Object target, String fieldName, Object value) {
		try {
			Field field = target.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}
}
