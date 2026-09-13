package com.withgahyo.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.withgahyo.domain.course.entity.Course;
import com.withgahyo.domain.course.entity.CourseStatus;
import com.withgahyo.domain.course.repository.CourseRepository;
import com.withgahyo.domain.place.entity.Region;
import com.withgahyo.domain.review.dto.CreateReviewRequest;
import com.withgahyo.domain.review.entity.Review;
import com.withgahyo.domain.review.entity.ReviewHighlight;
import com.withgahyo.domain.review.repository.ReviewHighlightRepository;
import com.withgahyo.domain.review.repository.ReviewRepository;
import com.withgahyo.domain.user.entity.User;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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

	@Mock
	private ReviewHighlightRepository reviewHighlightRepository;

	private ReviewService reviewService;

	@BeforeEach
	void setUp() {
		reviewService = new ReviewService(courseRepository, reviewRepository, reviewHighlightRepository);
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

	@Test
	void createReview_savesReviewAndHighlights() {
		User user = userWithId(User.create("KAKAO", "provider-user", "가효", null), 1L);
		Region region = Region.create("3", "1", "대전");
		Course course = courseWithId(
			Course.create(user, region, "대전 가족여행", LocalDate.of(2026, 6, 22), LocalDate.of(2026, 6, 23)),
			10L
		);
		setField(course, "status", CourseStatus.COMPLETED);
		CreateReviewRequest request = new CreateReviewRequest(
			(byte) 4,
			" 부모님과 함께 다녀오기 좋았어요. ",
			(byte) 9,
			List.of("쉬운 동선", "맛집")
		);

		given(courseRepository.findActiveById(10L)).willReturn(Optional.of(course));
		given(reviewRepository.existsByCourse_CourseIdAndUser_UserId(10L, 1L)).willReturn(false);
		given(reviewRepository.save(any(Review.class))).willAnswer(invocation -> {
			Review review = invocation.getArgument(0);
			setField(review, "reviewId", 100L);
			setField(review, "createdAt", LocalDateTime.of(2026, 9, 13, 12, 0));
			setField(review, "updatedAt", LocalDateTime.of(2026, 9, 13, 12, 0));
			return review;
		});

		var response = reviewService.createReview(1L, 10L, request);

		assertThat(response.reviewId()).isEqualTo(100L);
		assertThat(response.courseId()).isEqualTo(10L);
		assertThat(response.rating()).isEqualTo((byte) 4);
		assertThat(response.comment()).isEqualTo("부모님과 함께 다녀오기 좋았어요.");
		assertThat(response.recommendationScore()).isEqualTo((byte) 9);
		assertThat(response.highlights()).containsExactly("쉬운 동선", "맛집");
		verify(reviewRepository).save(any(Review.class));
		verify(reviewHighlightRepository).saveAll(anyList());
	}

	@Test
	void getReviewForm_returnsCourseInfoAndHighlightOptions() {
		User user = userWithId(User.create("KAKAO", "provider-user", "가효", null), 1L);
		Region region = Region.create("3", "1", "대전");
		Course course = courseWithId(
			Course.create(user, region, "대전 가족여행", LocalDate.of(2026, 6, 22), LocalDate.of(2026, 6, 23)),
			10L
		);
		setField(course, "imageUrl", "https://example.com/course.jpg");

		given(courseRepository.findActiveById(10L)).willReturn(Optional.of(course));

		var response = reviewService.getReviewForm(1L, 10L);

		assertThat(response.course().courseId()).isEqualTo(10L);
		assertThat(response.course().title()).isEqualTo("대전 가족여행");
		assertThat(response.course().period()).isEqualTo("2026. 06. 22 - 06. 23");
		assertThat(response.course().imageUrl()).isEqualTo("https://example.com/course.jpg");
		assertThat(response.ratingMin()).isEqualTo((byte) 1);
		assertThat(response.ratingMax()).isEqualTo((byte) 5);
		assertThat(response.recommendationMin()).isEqualTo((byte) 0);
		assertThat(response.recommendationMax()).isEqualTo((byte) 10);
		assertThat(response.highlightOptions()).containsExactly("여행 코스", "편의시설", "맛집", "기억", "교통", "추천할래요");
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
