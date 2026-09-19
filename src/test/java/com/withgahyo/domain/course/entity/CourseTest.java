package com.withgahyo.domain.course.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.withgahyo.domain.place.entity.Region;
import com.withgahyo.domain.user.entity.User;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * daysUntilTrip()은 CourseDetailResponse/HomeResponse가 공유하는 계산이다.
 * 오늘(LocalDate.now()) 기준 startDate까지의 signed day difference여야 하며,
 * 과거 날짜를 0으로 clamp하면 안 된다(지난 여행을 D-Day로 오인하게 되는 회귀 방지).
 */
class CourseTest {

	@Test
	void daysUntilTrip_returnsPositive_whenStartDateIsInFuture() {
		Course course = courseWithStartDate(LocalDate.now().plusDays(5));

		assertThat(course.daysUntilTrip()).isEqualTo(5L);
	}

	@Test
	void daysUntilTrip_returnsOne_whenStartDateIsTomorrow() {
		Course course = courseWithStartDate(LocalDate.now().plusDays(1));

		assertThat(course.daysUntilTrip()).isEqualTo(1L);
	}

	@Test
	void daysUntilTrip_returnsZero_whenStartDateIsToday() {
		Course course = courseWithStartDate(LocalDate.now());

		assertThat(course.daysUntilTrip()).isEqualTo(0L);
	}

	@Test
	void daysUntilTrip_returnsNegativeOne_whenStartDateWasYesterday() {
		Course course = courseWithStartDate(LocalDate.now().minusDays(1));

		assertThat(course.daysUntilTrip()).isEqualTo(-1L);
	}

	@Test
	void daysUntilTrip_returnsNegative_whenStartDateIsFarInThePast() {
		// 과거 날짜를 0으로 보정하지 않는다. Math.max(0, ...) 같은 clamp가 있으면 이 케이스가 깨진다.
		Course course = courseWithStartDate(LocalDate.now().minusDays(13));

		assertThat(course.daysUntilTrip()).isEqualTo(-13L);
	}

	@Test
	void daysUntilTrip_isNegative_regardlessOfCourseStatus() {
		// status 전이 정책은 이번 범위 밖이다. UPCOMING이어도 startDate가 지났으면 음수여야 한다.
		Course course = courseWithStartDate(LocalDate.now().minusDays(13));
		course.confirm();

		assertThat(course.getStatus()).isEqualTo(CourseStatus.UPCOMING);
		assertThat(course.daysUntilTrip()).isEqualTo(-13L);
	}

	private Course courseWithStartDate(LocalDate startDate) {
		User creator = User.create("KAKAO", "creator", "작성자", null);
		Region region = Region.create("3", "1", "대전광역시 동구");
		return Course.create(creator, region, "테스트 코스", startDate, startDate.plusDays(1));
	}
}
