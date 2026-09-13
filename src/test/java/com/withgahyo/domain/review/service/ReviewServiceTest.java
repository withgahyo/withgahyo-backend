package com.withgahyo.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.withgahyo.domain.course.entity.Course;
import com.withgahyo.domain.course.entity.CourseStatus;
import com.withgahyo.domain.course.repository.CourseRepository;
import com.withgahyo.domain.place.entity.Region;
import com.withgahyo.domain.review.repository.ReviewRepository;
import com.withgahyo.domain.user.entity.User;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

	@Mock
	private CourseRepository courseRepository;

	@Mock
	private ReviewRepository reviewRepository;

	private ReviewService reviewService;

	@BeforeEach
	void setUp() {
		reviewService = new ReviewService(courseRepository, reviewRepository);
	}

	@Test
	void getPendingReviews_returnsCompletedCoursesWithoutReview() {
		User user = userWithId(User.create("KAKAO", "provider-user", "가효", null), 1L);
		Region region = Region.create("3", "1", "대전");
		Course course = courseWithId(
			Course.create(user, region, "대전 가족여행", LocalDate.of(2026, 6, 22), LocalDate.of(2026, 6, 23)),
			10L
		);
		setField(course, "status", CourseStatus.COMPLETED);
		setField(course, "imageUrl", "https://example.com/course.jpg");

		given(courseRepository.findReviewPendingCourses(1L)).willReturn(List.of(course));

		var response = reviewService.getPendingReviews(1L);

		assertThat(response.reviews()).hasSize(1);
		assertThat(response.reviews().get(0).courseId()).isEqualTo(10L);
		assertThat(response.reviews().get(0).title()).isEqualTo("대전 가족여행");
		assertThat(response.reviews().get(0).period()).isEqualTo("2026. 06. 22 - 06. 23");
		assertThat(response.reviews().get(0).imageUrl()).isEqualTo("https://example.com/course.jpg");
	}

	private User userWithId(User user, Long userId) {
		return setField(user, "userId", userId);
	}

	private Course courseWithId(Course course, Long courseId) {
		return setField(course, "courseId", courseId);
	}

	private <T> T setField(T target, String fieldName, Object value) {
		try {
			Field field = target.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(target, value);
			return target;
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
