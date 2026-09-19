package com.withgahyo.domain.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.withgahyo.domain.course.entity.Course;
import com.withgahyo.domain.place.entity.Place;
import com.withgahyo.domain.place.entity.Region;
import com.withgahyo.domain.recommendation.dto.StartRecommendationRequest;
import com.withgahyo.domain.recommendation.entity.RecommendationJobStatus;
import com.withgahyo.domain.user.entity.User;
import com.withgahyo.infra.ai.RecommendationAiClient;
import com.withgahyo.infra.ai.dto.AiRecommendationGenerateRequest;
import com.withgahyo.infra.ai.dto.AiRecommendationGenerateResponse;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * imageUrl 보강이 JPA dirty checking 으로 실제 UPDATE 까지 반영되는지 검증한다.
 * Mockito 단위 테스트는 PlaceRepository 가 목이라 영속성 컨텍스트를 거치지 않으므로,
 * repository.save() 없이도 갱신이 flush 되는지는 이 통합 테스트로만 확인할 수 있다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecommendationServiceImageBackfillIntegrationTest {

	@Autowired
	private RecommendationService recommendationService;

	@Autowired
	private EntityManager entityManager;

	@MockitoBean
	private RecommendationAiClient recommendationAiClient;

	@Test
	void startGeneration_flushesBackfilledImageUrl_withoutExplicitSave() {
		Place place = persistPlace(null);
		Course course = persistCourse();
		Long placeId = place.getPlaceId();
		stubAiResponse(course, placeId, "https://tour.example.com/backfilled.jpg");

		var response = recommendationService.startGeneration(
			course.getCreatorUser().getUserId(),
			course.getCourseId(),
			new StartRecommendationRequest(null, List.of(), null)
		);
		assertThat(response.status()).isEqualTo(RecommendationJobStatus.COMPLETED);

		// flush 로 dirty checking 이 UPDATE 를 발행하게 하고, clear 후 DB 에서 다시 읽어 실제 반영을 확인한다.
		entityManager.flush();
		entityManager.clear();

		Place reloaded = entityManager.find(Place.class, placeId);
		assertThat(reloaded.getImageUrl()).isEqualTo("https://tour.example.com/backfilled.jpg");
	}

	@Test
	void startGeneration_doesNotOverwriteExistingImageUrl_inDatabase() {
		Place place = persistPlace("https://example.com/old.jpg");
		Course course = persistCourse();
		Long placeId = place.getPlaceId();
		stubAiResponse(course, placeId, "https://example.com/new.jpg");

		recommendationService.startGeneration(
			course.getCreatorUser().getUserId(),
			course.getCourseId(),
			new StartRecommendationRequest(null, List.of(), null)
		);

		entityManager.flush();
		entityManager.clear();

		Place reloaded = entityManager.find(Place.class, placeId);
		assertThat(reloaded.getImageUrl()).isEqualTo("https://example.com/old.jpg");
	}

	@Test
	void startGeneration_doesNotChangeOtherPlaceColumns_whenBackfillingImageUrl() {
		Place place = persistPlace(null);
		Course course = persistCourse();
		Long placeId = place.getPlaceId();
		stubAiResponse(course, placeId, "https://tour.example.com/backfilled.jpg");

		recommendationService.startGeneration(
			course.getCreatorUser().getUserId(),
			course.getCourseId(),
			new StartRecommendationRequest(null, List.of(), null)
		);

		entityManager.flush();
		entityManager.clear();

		Place reloaded = entityManager.find(Place.class, placeId);
		assertThat(reloaded.getImageUrl()).isEqualTo("https://tour.example.com/backfilled.jpg");
		// AI 응답에는 다른 이름/주소/카테고리/좌표가 담겨 있지만 기존 값이 유지되어야 한다.
		assertThat(reloaded.getName()).isEqualTo("보강대상장소");
		assertThat(reloaded.getAddress()).isEqualTo("부산광역시 영도구 기존주소");
		assertThat(reloaded.getCat1()).isEqualTo("TOURIST_ATTRACTION");
		assertThat(reloaded.getSource()).isEqualTo("KAKAO");
		assertThat(reloaded.getLatitude()).isEqualByComparingTo("35.0783000");
		assertThat(reloaded.getLongitude()).isEqualByComparingTo("129.0148000");
	}

	private void stubAiResponse(Course course, Long placeId, String aiImageUrl) {
		AiRecommendationGenerateResponse.ItemResponse item = new AiRecommendationGenerateResponse.ItemResponse(
			1,
			"AI가보낸다른이름",
			"CULTURE",
			"TOUR_API",
			placeId,
			null,
			"999999",
			"12",
			"AI가보낸다른주소",
			"6",
			"0",
			new BigDecimal("37.000000"),
			new BigDecimal("128.000000"),
			aiImageUrl,
			70,
			List.of()
		);
		AiRecommendationGenerateResponse aiResponse = new AiRecommendationGenerateResponse(
			null,
			course.getCourseId(),
			List.of(new AiRecommendationGenerateResponse.CandidateResponse(
				1,
				"보강 검증용 코스",
				"요약",
				"이유",
				new BigDecimal("90.0"),
				120,
				List.of(new AiRecommendationGenerateResponse.DayResponse(1, List.of(item))),
				List.of()
			))
		);
		given(recommendationAiClient.generate(any(AiRecommendationGenerateRequest.class))).willReturn(aiResponse);
	}

	private Place persistPlace(String imageUrl) {
		Place place = Place.create(
			"BACKFILL-" + System.nanoTime(),
			"12",
			"KAKAO",
			"TOURIST_ATTRACTION",
			persistRegion(),
			"보강대상장소",
			"부산광역시 영도구 기존주소",
			new BigDecimal("35.0783000"),
			new BigDecimal("129.0148000"),
			imageUrl
		);
		entityManager.persist(place);
		entityManager.flush();
		return place;
	}

	private Course persistCourse() {
		User creator = User.create("KAKAO", "backfill-" + System.nanoTime(), "보강테스터", null);
		entityManager.persist(creator);
		Course course = Course.create(
			creator,
			persistRegion(),
			"보강 검증 코스",
			LocalDate.now().plusDays(1),
			LocalDate.now().plusDays(2)
		);
		entityManager.persist(course);
		entityManager.flush();
		return course;
	}

	private Region persistRegion() {
		return entityManager.createQuery(
				"select r from Region r where r.areaCode = :areaCode and r.sigunguCode = :sigunguCode",
				Region.class
			)
			.setParameter("areaCode", "6")
			.setParameter("sigunguCode", "0")
			.getResultStream()
			.findFirst()
			.orElseGet(() -> {
				Region region = Region.create("6", "0", "부산");
				entityManager.persist(region);
				entityManager.flush();
				return region;
			});
	}
}
