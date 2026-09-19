package com.withgahyo.domain.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.withgahyo.domain.course.entity.Course;
import com.withgahyo.domain.place.entity.Place;
import com.withgahyo.domain.place.entity.Region;
import com.withgahyo.domain.recommendation.dto.RecommendationCandidatesResponse;
import com.withgahyo.domain.recommendation.entity.RecommendationCandidate;
import com.withgahyo.domain.recommendation.entity.RecommendationCandidateItem;
import com.withgahyo.domain.recommendation.entity.RecommendationJob;
import com.withgahyo.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 후보 대표 이미지 계산이 실제 JPQL 로 동작하는지, 그리고 후보/장소 개수가 늘어도
 * 쿼리 수가 증가하지 않는지(N+1 부재)를 Hibernate 통계로 검증한다.
 * Mockito 단위 테스트는 repository 가 목이라 join fetch 유효성과 실제 쿼리 수를 확인할 수 없다.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@Transactional
class RecommendationServiceThumbnailIntegrationTest {

	@Autowired
	private RecommendationService recommendationService;

	@Autowired
	private EntityManager entityManager;

	@Test
	void getCandidates_picksFirstPlaceImageInTravelOrder() {
		Fixture fixture = persistJobWithCandidates(1, 3);
		// 후보의 1일차 1번 장소만 이미지가 없고, 1일차 2번 장소에 이미지가 있다.
		setPlaceImageUrls(fixture.candidates().get(0), null, "https://example.com/second.jpg", null);
		entityManager.flush();
		entityManager.clear();

		RecommendationCandidatesResponse response = recommendationService.getCandidates(
			fixture.userId(),
			fixture.jobId()
		);

		assertThat(response.candidates()).hasSize(1);
		assertThat(response.candidates().get(0).thumbnailImageUrl()).isEqualTo("https://example.com/second.jpg");
	}

	@Test
	void getCandidates_returnsNullThumbnail_whenNoPlaceHasImage() {
		Fixture fixture = persistJobWithCandidates(1, 3);
		setPlaceImageUrls(fixture.candidates().get(0), null, "", "   ");
		entityManager.flush();
		entityManager.clear();

		RecommendationCandidatesResponse response = recommendationService.getCandidates(
			fixture.userId(),
			fixture.jobId()
		);

		assertThat(response.candidates().get(0).thumbnailImageUrl()).isNull();
	}

	@Test
	void getCandidates_doesNotIssueExtraQueriesPerCandidate() {
		Fixture oneCandidate = persistJobWithCandidates(1, 3);
		setPlaceImageUrls(oneCandidate.candidates().get(0), null, "https://example.com/one.jpg", null);
		Fixture threeCandidates = persistJobWithCandidates(3, 3);
		for (RecommendationCandidate candidate : threeCandidates.candidates()) {
			setPlaceImageUrls(candidate, null, "https://example.com/many.jpg", null);
		}
		entityManager.flush();
		entityManager.clear();

		long queriesForOne = countQueries(() -> recommendationService.getCandidates(
			oneCandidate.userId(),
			oneCandidate.jobId()
		));
		entityManager.clear();
		long queriesForThree = countQueries(() -> recommendationService.getCandidates(
			threeCandidates.userId(),
			threeCandidates.jobId()
		));

		// 후보가 1개일 때와 3개일 때 쿼리 수가 같아야 한다(후보/장소 수에 비례한 추가 쿼리 없음).
		assertThat(queriesForThree).isEqualTo(queriesForOne);
	}

	private long countQueries(Runnable action) {
		Statistics statistics = entityManager.getEntityManagerFactory()
			.unwrap(SessionFactory.class)
			.getStatistics();
		statistics.clear();
		action.run();
		return statistics.getPrepareStatementCount();
	}

	private void setPlaceImageUrls(RecommendationCandidate candidate, String... imageUrls) {
		List<RecommendationCandidateItem> items = entityManager.createQuery("""
				select rci from RecommendationCandidateItem rci
				join fetch rci.place
				where rci.recommendationCandidate = :candidate
				order by rci.dayNumber asc, rci.visitOrder asc
				""", RecommendationCandidateItem.class)
			.setParameter("candidate", candidate)
			.getResultList();
		for (int index = 0; index < imageUrls.length && index < items.size(); index++) {
			Place place = items.get(index).getPlace();
			// 테스트 픽스처 세팅이므로 도메인 보강 메서드를 우회해 원하는 상태를 직접 만든다.
			org.springframework.test.util.ReflectionTestUtils.setField(place, "imageUrl", imageUrls[index]);
		}
	}

	private Fixture persistJobWithCandidates(int candidateCount, int itemsPerCandidate) {
		User creator = User.create("KAKAO", "thumb-" + System.nanoTime(), "썸네일테스터", null);
		entityManager.persist(creator);
		Region region = findOrPersistRegion();
		Course course = Course.create(
			creator,
			region,
			"썸네일 검증 코스",
			LocalDate.now().plusDays(1),
			LocalDate.now().plusDays(2)
		);
		entityManager.persist(course);

		RecommendationJob job = RecommendationJob.createPending(course);
		job.start();
		job.complete();
		entityManager.persist(job);

		List<RecommendationCandidate> candidates = new java.util.ArrayList<>();
		for (int rank = 1; rank <= candidateCount; rank++) {
			RecommendationCandidate candidate = RecommendationCandidate.create(
				job,
				rank,
				"후보 " + rank,
				"설명",
				new BigDecimal("90.00"),
				1000,
				30
			);
			entityManager.persist(candidate);
			candidates.add(candidate);
			for (int visitOrder = 1; visitOrder <= itemsPerCandidate; visitOrder++) {
				entityManager.persist(RecommendationCandidateItem.create(
					candidate,
					persistPlace(region),
					1,
					visitOrder,
					null,
					null,
					null,
					null,
					null
				));
			}
		}
		entityManager.flush();
		return new Fixture(creator.getUserId(), job.getRecommendationJobId(), candidates);
	}

	private Place persistPlace(Region region) {
		Place place = Place.create(
			"THUMB-" + System.nanoTime(),
			"12",
			"KAKAO",
			"TOURIST_ATTRACTION",
			region,
			"썸네일 검증 장소",
			"부산광역시 영도구 테스트주소",
			new BigDecimal("35.0783000"),
			new BigDecimal("129.0148000"),
			null
		);
		entityManager.persist(place);
		return place;
	}

	private Region findOrPersistRegion() {
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

	private record Fixture(Long userId, Long jobId, List<RecommendationCandidate> candidates) {
	}
}
