package com.withgahyo.domain.onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.withgahyo.domain.onboarding.dto.ConditionsRequest;
import com.withgahyo.domain.onboarding.dto.OnboardingResponse;
import com.withgahyo.domain.onboarding.dto.OnboardingSaveRequest;
import com.withgahyo.domain.onboarding.exception.OnboardingErrorCode;
import com.withgahyo.domain.user.entity.User;
import com.withgahyo.domain.user.entity.UserOnboardingProfile;
import com.withgahyo.domain.user.repository.UserOnboardingProfileRepository;
import com.withgahyo.domain.user.repository.UserRepository;
import com.withgahyo.global.exception.BusinessException;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 실제 JPA/Hibernate 영속성 동작(@MapsId 공유 PK, Spring Data isNew(), persist vs merge)을 검증한다.
 * Mockito 단위 테스트로는 잡히지 않던 "신규 사용자 최초 온보딩 저장 500" 회귀를 방지한다.
 *
 * application-test.yml 은 data.sql 을 실행하지 않으므로, 필요한 취향 마스터는 테스트가 직접 등록한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OnboardingServiceIntegrationTest {

	@Autowired
	private OnboardingService onboardingService;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private UserOnboardingProfileRepository userOnboardingProfileRepository;
	@Autowired
	private EntityManager entityManager;

	private long tourismId1;
	private long tourismId2;
	private long foodId1;
	private long foodId2;

	@BeforeEach
	void setUpMasterData() {
		tourismId1 = upsertTourismPreference("IT_NATURE", "자연/풍경");
		tourismId2 = upsertTourismPreference("IT_HISTORY", "역사/유적");
		foodId1 = upsertFoodPreference("IT_KOREAN", "한식");
		foodId2 = upsertFoodPreference("IT_SPICY", "매운 맛");
	}

	@Test
	void saveOnboarding_createsProfile_forNewUserWithoutProfile() {
		Long userId = createUser();
		assertThat(userOnboardingProfileRepository.findById(userId)).isEmpty();

		OnboardingSaveRequest request = new OnboardingSaveRequest(
			"under-30min", "moderate", "avoid", null, "a-little",
			List.of(tourismId1, tourismId2), List.of(foodId1, foodId2)
		);

		onboardingService.saveOnboarding(userId, request);
		flushAndClear();

		OnboardingResponse response = onboardingService.getOnboarding(userId);
		assertThat(response.walkingTolerance()).isEqualTo("under-30min");
		assertThat(response.restPreference()).isEqualTo("moderate");
		assertThat(response.stairsPreference()).isEqualTo("avoid");
		assertThat(response.slopePreference()).isNull();
		assertThat(response.spicyPreference()).isEqualTo("a-little");
		assertThat(response.tourismPreferenceIds()).containsExactlyInAnyOrder(tourismId1, tourismId2);
		assertThat(response.foodPreferenceIds()).containsExactlyInAnyOrder(foodId1, foodId2);

		assertThat(userOnboardingProfileRepository.findById(userId)).isPresent();
		assertThat(profileRowCount(userId)).isEqualTo(1L);
		assertThat(tourismMappingRowCount(userId)).isEqualTo(2L);
		assertThat(foodMappingRowCount(userId)).isEqualTo(2L);
	}

	@Test
	void saveOnboarding_succeeds_whenAllConditionsAreNull_forNewUser() {
		Long userId = createUser();

		OnboardingSaveRequest request = new OnboardingSaveRequest(
			null, null, null, null, null,
			List.of(tourismId1), List.of(foodId1)
		);

		assertThatCode(() -> onboardingService.saveOnboarding(userId, request)).doesNotThrowAnyException();
		flushAndClear();

		OnboardingResponse response = onboardingService.getOnboarding(userId);
		assertThat(response.walkingTolerance()).isNull();
		assertThat(response.restPreference()).isNull();
		assertThat(response.stairsPreference()).isNull();
		assertThat(response.slopePreference()).isNull();
		assertThat(response.spicyPreference()).isNull();
		assertThat(profileRowCount(userId)).isEqualTo(1L);
	}

	@Test
	void saveOnboarding_updatesExistingProfile_withoutCreatingDuplicateRow() {
		Long userId = createUser();
		onboardingService.saveOnboarding(userId, new OnboardingSaveRequest(
			"under-30min", "moderate", "avoid", null, "a-little",
			List.of(tourismId1), List.of(foodId1)
		));
		flushAndClear();

		onboardingService.saveOnboarding(userId, new OnboardingSaveRequest(
			"over-1hour", "none", "none", null, "none",
			List.of(tourismId2), List.of(foodId2)
		));
		flushAndClear();

		OnboardingResponse response = onboardingService.getOnboarding(userId);
		assertThat(response.walkingTolerance()).isEqualTo("over-1hour");
		assertThat(response.tourismPreferenceIds()).containsExactly(tourismId2);
		assertThat(response.foodPreferenceIds()).containsExactly(foodId2);
		assertThat(profileRowCount(userId)).isEqualTo(1L);
		assertThat(tourismMappingRowCount(userId)).isEqualTo(1L);
		assertThat(foodMappingRowCount(userId)).isEqualTo(1L);
	}

	@Test
	void saveOnboarding_throwsInvalidTourismPreference_forNewUser_notServerError() {
		Long userId = createUser();

		OnboardingSaveRequest request = new OnboardingSaveRequest(
			null, null, null, null, null,
			List.of(999_999L), List.of(foodId1)
		);

		assertThatThrownBy(() -> onboardingService.saveOnboarding(userId, request))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
				.isEqualTo(OnboardingErrorCode.INVALID_TOURISM_PREFERENCE));
	}

	@Test
	void completeOnboarding_completes_forNewUser_afterSave() {
		Long userId = createUser();
		onboardingService.saveOnboarding(userId, new OnboardingSaveRequest(
			"under-30min", "moderate", "avoid", null, "a-little",
			List.of(tourismId1), List.of(foodId1)
		));
		flushAndClear();

		onboardingService.completeOnboarding(userId);
		flushAndClear();

		assertThat(onboardingService.getOnboarding(userId).onboardingCompleted()).isTrue();
	}

	@Test
	void completeOnboarding_isIdempotent_whenAlreadyCompleted() {
		Long userId = createUser();
		onboardingService.saveOnboarding(userId, new OnboardingSaveRequest(
			null, null, null, null, null,
			List.of(tourismId1), List.of(foodId1)
		));
		onboardingService.completeOnboarding(userId);
		flushAndClear();

		assertThatCode(() -> onboardingService.completeOnboarding(userId)).doesNotThrowAnyException();
		assertThat(onboardingService.getOnboarding(userId).onboardingCompleted()).isTrue();
	}

	@Test
	void saveConditions_createsProfile_forNewUserWithoutProfile() {
		Long userId = createUser();
		assertThat(userOnboardingProfileRepository.findById(userId)).isEmpty();

		onboardingService.saveConditions(userId, new ConditionsRequest(
			"under-10min", "frequent", "avoid", null, "none"
		));
		flushAndClear();

		UserOnboardingProfile profile = userOnboardingProfileRepository.findById(userId).orElseThrow();
		assertThat(profile.getWalkingTolerance()).isEqualTo("under-10min");
		assertThat(profile.getRestPreference()).isEqualTo("frequent");
		assertThat(profile.getStairsPreference()).isEqualTo("avoid");
		assertThat(profile.getSlopePreference()).isNull();
		assertThat(profile.getSpicyPreference()).isEqualTo("none");
		assertThat(profileRowCount(userId)).isEqualTo(1L);
	}

	private Long createUser() {
		User user = userRepository.save(User.create(
			"KAKAO", "it-" + System.nanoTime(), "it@example.com", "통합테스트", null
		));
		entityManager.flush();
		return user.getUserId();
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}

	private long upsertTourismPreference(String code, String name) {
		entityManager.createNativeQuery(
				"INSERT INTO tourism_preference (code, name, is_active) VALUES (?, ?, true) "
					+ "ON DUPLICATE KEY UPDATE name = VALUES(name), is_active = true")
			.setParameter(1, code)
			.setParameter(2, name)
			.executeUpdate();
		return ((Number) entityManager.createNativeQuery(
				"SELECT tourism_preference_id FROM tourism_preference WHERE code = ?")
			.setParameter(1, code)
			.getSingleResult()).longValue();
	}

	private long upsertFoodPreference(String code, String name) {
		entityManager.createNativeQuery(
				"INSERT INTO food_preference (code, name, is_active) VALUES (?, ?, true) "
					+ "ON DUPLICATE KEY UPDATE name = VALUES(name), is_active = true")
			.setParameter(1, code)
			.setParameter(2, name)
			.executeUpdate();
		return ((Number) entityManager.createNativeQuery(
				"SELECT food_preference_id FROM food_preference WHERE code = ?")
			.setParameter(1, code)
			.getSingleResult()).longValue();
	}

	private long profileRowCount(Long userId) {
		return ((Number) entityManager.createNativeQuery(
				"SELECT COUNT(*) FROM user_onboarding_profile WHERE user_id = ?")
			.setParameter(1, userId)
			.getSingleResult()).longValue();
	}

	private long tourismMappingRowCount(Long userId) {
		return ((Number) entityManager.createNativeQuery(
				"SELECT COUNT(*) FROM user_tourism_preference WHERE user_id = ?")
			.setParameter(1, userId)
			.getSingleResult()).longValue();
	}

	private long foodMappingRowCount(Long userId) {
		return ((Number) entityManager.createNativeQuery(
				"SELECT COUNT(*) FROM user_food_preference WHERE user_id = ?")
			.setParameter(1, userId)
			.getSingleResult()).longValue();
	}
}
