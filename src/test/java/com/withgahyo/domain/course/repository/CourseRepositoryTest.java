package com.withgahyo.domain.course.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.withgahyo.domain.course.entity.Course;
import com.withgahyo.domain.course.entity.CourseParticipant;
import com.withgahyo.domain.place.entity.Region;
import com.withgahyo.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * Home 캐러셀용 CourseRepository.findUpcomingCoursesForUser 를 실제 DB로 검증한다.
 * 이 프로젝트에는 @DataJpaTest 선례가 없어, 기존 통합 테스트와 동일한
 * @SpringBootTest + @ActiveProfiles("test") + @Transactional 패턴을 그대로 따른다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CourseRepositoryTest {

	@Autowired
	private CourseRepository courseRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void findUpcomingCoursesForUser_includesCourse_whenUserIsCreator() {
		User creator = persistUser();
		Course upcoming = persistCourse(creator, "생성자 코스", LocalDate.now().plusDays(5));
		upcoming.confirm();
		entityManager.flush();

		List<Course> result = courseRepository.findUpcomingCoursesForUser(creator.getUserId());

		assertThat(result).extracting(Course::getCourseId).containsExactly(upcoming.getCourseId());
	}

	@Test
	void findUpcomingCoursesForUser_includesCourse_whenUserIsParticipant() {
		User creator = persistUser();
		User participant = persistUser();
		Course upcoming = persistCourse(creator, "참여자 코스", LocalDate.now().plusDays(5));
		upcoming.confirm();
		entityManager.persist(CourseParticipant.create(upcoming, participant, "MOTHER"));
		entityManager.flush();

		List<Course> result = courseRepository.findUpcomingCoursesForUser(participant.getUserId());

		assertThat(result).extracting(Course::getCourseId).containsExactly(upcoming.getCourseId());
	}

	@Test
	void findUpcomingCoursesForUser_excludesDraftCourse() {
		User creator = persistUser();
		persistCourse(creator, "초안 코스", LocalDate.now().plusDays(5)); // confirm() 호출 안 함 -> DRAFT
		entityManager.flush();

		List<Course> result = courseRepository.findUpcomingCoursesForUser(creator.getUserId());

		assertThat(result).isEmpty();
	}

	@Test
	void findUpcomingCoursesForUser_excludesCanceledCourse() {
		User creator = persistUser();
		Course canceled = persistCourse(creator, "취소된 코스", LocalDate.now().plusDays(5));
		canceled.confirm();
		canceled.softDelete();
		entityManager.flush();

		List<Course> result = courseRepository.findUpcomingCoursesForUser(creator.getUserId());

		assertThat(result).isEmpty();
	}

	@Test
	void findUpcomingCoursesForUser_excludesDeletedCourse() {
		User creator = persistUser();
		Course deleted = persistCourse(creator, "삭제된 코스", LocalDate.now().plusDays(5));
		deleted.confirm();
		// softDelete()는 status를 CANCELED로 함께 바꾸므로, "삭제 여부"만 독립적으로 검증하기 위해
		// status는 UPCOMING으로 유지한 채 deletedAt만 강제로 채운다.
		ReflectionTestUtils.setField(deleted, "deletedAt", java.time.LocalDateTime.now());
		entityManager.flush();

		List<Course> result = courseRepository.findUpcomingCoursesForUser(creator.getUserId());

		assertThat(result).isEmpty();
	}

	@Test
	void findUpcomingCoursesForUser_ordersByStartDateAscending() {
		User creator = persistUser();
		Course later = persistCourse(creator, "늦은 여행", LocalDate.now().plusDays(20));
		Course sooner = persistCourse(creator, "가까운 여행", LocalDate.now().plusDays(3));
		later.confirm();
		sooner.confirm();
		entityManager.flush();

		List<Course> result = courseRepository.findUpcomingCoursesForUser(creator.getUserId());

		assertThat(result).extracting(Course::getCourseId)
			.containsExactly(sooner.getCourseId(), later.getCourseId());
	}

	@Test
	void findUpcomingCoursesForUser_doesNotReturnDuplicate_whenUserIsBothCreatorAndParticipant() {
		User creator = persistUser();
		Course upcoming = persistCourse(creator, "생성자 겸 참여자 코스", LocalDate.now().plusDays(5));
		upcoming.confirm();
		// 실제 생성 플로우에서는 발생하지 않는 조합이지만, creator이면서 동시에 participant row도 있는
		// 극단적인 경우에도 EXISTS 서브쿼리 특성상 같은 Course가 두 번 반환되지 않아야 한다.
		entityManager.persist(CourseParticipant.create(upcoming, creator, "SELF"));
		entityManager.flush();

		List<Course> result = courseRepository.findUpcomingCoursesForUser(creator.getUserId());

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getCourseId()).isEqualTo(upcoming.getCourseId());
	}

	// ---- findUpcomingCoursesFromDateForUser (날씨 기능용: 과거 UPCOMING 코스 제외) ----

	@Test
	void findUpcomingCoursesFromDateForUser_includesCourse_whenUserIsCreator() {
		User creator = persistUser();
		Course upcoming = persistCourse(creator, "생성자 코스", LocalDate.now().plusDays(5));
		upcoming.confirm();
		entityManager.flush();

		List<Course> result = courseRepository.findUpcomingCoursesFromDateForUser(creator.getUserId(), LocalDate.now());

		assertThat(result).extracting(Course::getCourseId).containsExactly(upcoming.getCourseId());
	}

	@Test
	void findUpcomingCoursesFromDateForUser_includesCourse_whenUserIsParticipant() {
		User creator = persistUser();
		User participant = persistUser();
		Course upcoming = persistCourse(creator, "참여자 코스", LocalDate.now().plusDays(5));
		upcoming.confirm();
		entityManager.persist(CourseParticipant.create(upcoming, participant, "MOTHER"));
		entityManager.flush();

		List<Course> result =
			courseRepository.findUpcomingCoursesFromDateForUser(participant.getUserId(), LocalDate.now());

		assertThat(result).extracting(Course::getCourseId).containsExactly(upcoming.getCourseId());
	}

	@Test
	void findUpcomingCoursesFromDateForUser_excludesDraftCourse() {
		User creator = persistUser();
		persistCourse(creator, "초안 코스", LocalDate.now().plusDays(5)); // confirm() 호출 안 함 -> DRAFT
		entityManager.flush();

		List<Course> result = courseRepository.findUpcomingCoursesFromDateForUser(creator.getUserId(), LocalDate.now());

		assertThat(result).isEmpty();
	}

	@Test
	void findUpcomingCoursesFromDateForUser_excludesDeletedCourse() {
		User creator = persistUser();
		Course deleted = persistCourse(creator, "삭제된 코스", LocalDate.now().plusDays(5));
		deleted.confirm();
		deleted.softDelete();
		entityManager.flush();

		List<Course> result = courseRepository.findUpcomingCoursesFromDateForUser(creator.getUserId(), LocalDate.now());

		assertThat(result).isEmpty();
	}

	@Test
	void findUpcomingCoursesFromDateForUser_excludesPastStartDate() {
		// Home용 findUpcomingCoursesForUser와의 핵심 차이: startDate가 지난 UPCOMING 코스는
		// 날씨 기능에서 "예정 여행"이 아니므로 제외돼야 한다.
		User creator = persistUser();
		Course past = persistCourse(creator, "지난 여행", LocalDate.now().minusDays(3));
		past.confirm();
		entityManager.flush();

		List<Course> result = courseRepository.findUpcomingCoursesFromDateForUser(creator.getUserId(), LocalDate.now());

		assertThat(result).isEmpty();
	}

	@Test
	void findUpcomingCoursesFromDateForUser_includesCourse_whenStartDateIsToday() {
		User creator = persistUser();
		Course today = persistCourse(creator, "오늘 출발", LocalDate.now());
		today.confirm();
		entityManager.flush();

		List<Course> result = courseRepository.findUpcomingCoursesFromDateForUser(creator.getUserId(), LocalDate.now());

		assertThat(result).extracting(Course::getCourseId).containsExactly(today.getCourseId());
	}

	@Test
	void findUpcomingCoursesFromDateForUser_selectsClosestAmongMultipleFutureCourses() {
		User creator = persistUser();
		Course later = persistCourse(creator, "늦은 여행", LocalDate.now().plusDays(20));
		Course sooner = persistCourse(creator, "가까운 여행", LocalDate.now().plusDays(3));
		later.confirm();
		sooner.confirm();
		entityManager.flush();

		List<Course> result = courseRepository.findUpcomingCoursesFromDateForUser(creator.getUserId(), LocalDate.now());

		assertThat(result.get(0).getCourseId()).isEqualTo(sooner.getCourseId());
	}

	@Test
	void findUpcomingCoursesFromDateForUser_ordersDeterministically_whenSameStartDate() {
		User creator = persistUser();
		LocalDate sameDate = LocalDate.now().plusDays(7);
		Course first = persistCourse(creator, "같은 날 코스 A", sameDate);
		Course second = persistCourse(creator, "같은 날 코스 B", sameDate);
		first.confirm();
		second.confirm();
		entityManager.flush();

		List<Course> result = courseRepository.findUpcomingCoursesFromDateForUser(creator.getUserId(), LocalDate.now());

		// startDate가 같으면 courseId 오름차순으로 항상 같은 순서여야 한다.
		Long expectedFirstId = Math.min(first.getCourseId(), second.getCourseId());
		assertThat(result.get(0).getCourseId()).isEqualTo(expectedFirstId);
	}

	@Test
	void findUpcomingCoursesFromDateForUser_returnsEmpty_whenNoUpcomingTrip() {
		User creator = persistUser();

		List<Course> result = courseRepository.findUpcomingCoursesFromDateForUser(creator.getUserId(), LocalDate.now());

		assertThat(result).isEmpty();
	}

	private User persistUser() {
		User user = User.create("KAKAO", "home-test-" + System.nanoTime(), "홈테스터", null);
		entityManager.persist(user);
		return user;
	}

	private Course persistCourse(User creator, String title, LocalDate startDate) {
		Course course = Course.create(creator, findOrPersistRegion(), title, startDate, startDate.plusDays(1));
		entityManager.persist(course);
		return course;
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
