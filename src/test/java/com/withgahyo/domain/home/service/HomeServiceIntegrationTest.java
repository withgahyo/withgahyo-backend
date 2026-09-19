package com.withgahyo.domain.home.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.withgahyo.domain.course.entity.Course;
import com.withgahyo.domain.course.entity.CourseInterestKeyword;
import com.withgahyo.domain.course.entity.CourseKeyword;
import com.withgahyo.domain.course.entity.CourseScheduleItem;
import com.withgahyo.domain.home.dto.HomeResponse;
import com.withgahyo.domain.place.entity.Place;
import com.withgahyo.domain.place.entity.Region;
import com.withgahyo.domain.recommendation.entity.RecommendationCandidate;
import com.withgahyo.domain.recommendation.entity.RecommendationCandidateItem;
import com.withgahyo.domain.recommendation.entity.RecommendationJob;
import com.withgahyo.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * HomeService.getHome() 이 실제 JPQL(join fetch)로 정상 동작하고,
 * Course 개수가 늘어도 쿼리 수가 늘지 않는지(N+1 부재)를 Hibernate 통계로 검증한다.
 * 새 인프라 없이 RecommendationServiceThumbnailIntegrationTest 와 동일한 방식을 재사용한다.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@Transactional
class HomeServiceIntegrationTest {

	@Autowired
	private HomeService homeService;

	@Autowired
	private EntityManager entityManager;

	@Test
	void getHome_returnsTagsAndThumbnail_fromRealQueries() {
		User user = persistUser();
		Region region = findOrPersistRegion();
		Course course = persistConfirmedCourse(user, region, "대전 가족 여행", LocalDate.now().plusDays(6));
		persistKeyword(course, "BARRIER_FREE", "무장애");
		persistScheduleItem(course, region, 1, 1, null);
		persistScheduleItem(course, region, 1, 2, "https://example.com/place.jpg");
		entityManager.flush();
		entityManager.clear();

		HomeResponse response = homeService.getHome(user.getUserId());

		assertThat(response.familyCourses()).hasSize(1);
		HomeResponse.FamilyCourseResponse familyCourse = response.familyCourses().get(0);
		assertThat(familyCourse.tags()).containsExactly("무장애");
		assertThat(familyCourse.imageUrl()).isEqualTo("https://example.com/place.jpg");
	}

	@Test
	void getHome_returnsAlternativesFromConfirmedJobOnly_realQuery() {
		// Course 하나에 재시도로 생긴 Job이 2개(첫 번째는 확정에 쓰이지 않고 버려짐) 존재하는 상황을
		// 실제 DB에 그대로 만들어, JPQL(join fetch)이 selected=true가 속한 job만 정확히 골라내는지 확인한다.
		User user = persistUser();
		Region region = findOrPersistRegion();
		Course course = persistConfirmedCourse(user, region, "대전 가족 여행", LocalDate.now().plusDays(6));

		RecommendationJob abandonedJob = persistJob(course);
		persistCandidate(abandonedJob, 1, "A", "요약A", false, null);
		persistCandidate(abandonedJob, 2, "B", "요약B", false, null);

		RecommendationJob confirmedJob = persistJob(course);
		persistCandidate(confirmedJob, 1, "D", "요약D", false, "https://example.com/d.jpg");
		persistCandidate(confirmedJob, 2, "E", "요약E", true, null);
		persistCandidate(confirmedJob, 3, "F", "요약F", false, null);

		entityManager.flush();
		entityManager.clear();

		HomeResponse response = homeService.getHome(user.getUserId());

		assertThat(response.familyCourses()).hasSize(1);
		var alternatives = response.familyCourses().get(0).alternativeCandidates();
		assertThat(alternatives).extracting(HomeResponse.AlternativeCandidateResponse::title)
			.containsExactly("D", "F");
		assertThat(alternatives).extracting(HomeResponse.AlternativeCandidateResponse::thumbnailImageUrl)
			.containsExactly("https://example.com/d.jpg", null);
	}

	@Test
	void getHome_doesNotIssueExtraQueriesPerCourse() {
		User user = persistUser();
		Region region = findOrPersistRegion();
		Course singleCourse = persistConfirmedCourse(user, region, "코스 1개", LocalDate.now().plusDays(3));
		persistKeyword(singleCourse, "BARRIER_FREE", "무장애");
		persistScheduleItem(singleCourse, region, 1, 1, "https://example.com/a.jpg");
		persistSelectedCandidateWithAlternatives(singleCourse, "https://example.com/single-alt.jpg");

		User anotherUser = persistUser();
		CourseInterestKeyword sharedKeyword = persistKeywordMaster("FOOD_TOUR_" + System.nanoTime(), "맛집");
		for (int i = 0; i < 3; i++) {
			Course course = persistConfirmedCourse(anotherUser, region, "코스 " + i, LocalDate.now().plusDays(i + 10));
			entityManager.persist(CourseKeyword.create(course, sharedKeyword));
			persistScheduleItem(course, region, 1, 1, "https://example.com/" + i + ".jpg");
			persistSelectedCandidateWithAlternatives(course, "https://example.com/alt-" + i + ".jpg");
		}
		entityManager.flush();
		entityManager.clear();

		long queriesForOneCourse = countQueries(() -> homeService.getHome(user.getUserId()));
		entityManager.clear();
		long queriesForThreeCourses = countQueries(() -> homeService.getHome(anotherUser.getUserId()));

		// Course 가 1개일 때와 3개일 때 쿼리 수가 같아야 한다(Course 수에 비례한 추가 쿼리 없음).
		// alternativeCandidates 조회(job/candidate/candidate-item batch)까지 포함해서 검증한다.
		assertThat(queriesForThreeCourses).isEqualTo(queriesForOneCourse);
	}

	private void persistSelectedCandidateWithAlternatives(Course course, String alternativeThumbnailUrl) {
		RecommendationJob job = persistJob(course);
		persistCandidate(job, 1, "선택된 후보", "요약", true, null);
		persistCandidate(job, 2, "대안 후보", "요약", false, alternativeThumbnailUrl);
	}

	private RecommendationJob persistJob(Course course) {
		RecommendationJob job = RecommendationJob.createPending(course);
		job.start();
		job.complete();
		entityManager.persist(job);
		return job;
	}

	private RecommendationCandidate persistCandidate(
		RecommendationJob job,
		int rank,
		String title,
		String description,
		boolean selected,
		String thumbnailImageUrl
	) {
		RecommendationCandidate candidate = RecommendationCandidate.create(
			job, rank, title, description, new BigDecimal("90.0"), null, 60
		);
		entityManager.persist(candidate);
		if (selected) {
			candidate.select();
		}
		if (thumbnailImageUrl != null) {
			Place place = Place.create(
				"HOME-IT-CANDIDATE-" + System.nanoTime(), "12", "KAKAO", "TOURIST_ATTRACTION",
				job.getCourse().getRegion(), "후보 장소", "대전광역시 서구 어딘가",
				new BigDecimal("36.366"), new BigDecimal("127.388"), thumbnailImageUrl
			);
			entityManager.persist(place);
			entityManager.persist(RecommendationCandidateItem.create(
				candidate, place, 1, 1, null, null, null, null, null
			));
		}
		return candidate;
	}

	private long countQueries(Runnable action) {
		Statistics statistics = entityManager.getEntityManagerFactory()
			.unwrap(SessionFactory.class)
			.getStatistics();
		statistics.clear();
		action.run();
		return statistics.getPrepareStatementCount();
	}

	private User persistUser() {
		User user = User.create("KAKAO", "home-it-" + System.nanoTime(), "홈통합테스터", null);
		entityManager.persist(user);
		return user;
	}

	private Course persistConfirmedCourse(User creator, Region region, String title, LocalDate startDate) {
		Course course = Course.create(creator, region, title, startDate, startDate.plusDays(1));
		entityManager.persist(course);
		course.confirm();
		return course;
	}

	private void persistKeyword(Course course, String code, String name) {
		entityManager.persist(CourseKeyword.create(course, persistKeywordMaster(code, name)));
	}

	private CourseInterestKeyword persistKeywordMaster(String code, String name) {
		CourseInterestKeyword keyword = CourseInterestKeyword.create(code, name);
		entityManager.persist(keyword);
		return keyword;
	}

	private void persistScheduleItem(Course course, Region region, int dayNumber, int visitOrder, String imageUrl) {
		Place place = Place.create(
			"HOME-IT-" + System.nanoTime(),
			"12",
			"KAKAO",
			"TOURIST_ATTRACTION",
			region,
			"홈 통합 테스트 장소",
			"대전광역시 서구 어딘가",
			new BigDecimal("36.366"),
			new BigDecimal("127.388"),
			imageUrl
		);
		entityManager.persist(place);
		entityManager.persist(CourseScheduleItem.create(
			course, place, dayNumber, visitOrder, null, null, null, null, null
		));
	}

	private Region findOrPersistRegion() {
		return entityManager.createQuery(
				"select r from Region r where r.areaCode = :areaCode and r.sigunguCode = :sigunguCode",
				Region.class
			)
			.setParameter("areaCode", "3")
			.setParameter("sigunguCode", "1")
			.getResultStream()
			.findFirst()
			.orElseGet(() -> {
				Region region = Region.create("3", "1", "대전광역시 동구");
				entityManager.persist(region);
				entityManager.flush();
				return region;
			});
	}
}
