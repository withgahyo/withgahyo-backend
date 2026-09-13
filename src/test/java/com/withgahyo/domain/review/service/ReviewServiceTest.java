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
import com.withgahyo.domain.review.dto.UpdateReviewRequest;
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

	@Test
	void getMyReview_returnsReviewWithHighlights() {
		User user = userWithId(User.create("KAKAO", "provider-user", "가효", null), 1L);
		Region region = Region.create("3", "1", "대전");
		Course course = courseWithId(
			Course.create(user, region, "대전 가족여행", LocalDate.of(2026, 6, 22), LocalDate.of(2026, 6, 23)),
			10L
		);
		Review review = reviewWithId(
			Review.create(course, user, (byte) 5, "정말 만족스러웠어요.", (byte) 10),
			100L
		);

		given(reviewRepository.findByCourse_CourseIdAndUser_UserId(10L, 1L)).willReturn(Optional.of(review));
		given(reviewHighlightRepository.findByReview_ReviewId(100L)).willReturn(List.of(
			ReviewHighlight.create(review, "여행 코스"),
			ReviewHighlight.create(review, "맛집")
		));

		var response = reviewService.getMyReview(1L, 10L);

		assertThat(response.reviewId()).isEqualTo(100L);
		assertThat(response.courseId()).isEqualTo(10L);
		assertThat(response.rating()).isEqualTo((byte) 5);
		assertThat(response.comment()).isEqualTo("정말 만족스러웠어요.");
		assertThat(response.recommendationScore()).isEqualTo((byte) 10);
		assertThat(response.highlights()).containsExactly("여행 코스", "맛집");
	}

	@Test
	void getMyReviews_returnsWrittenReviewsWithCourseInfoAndHighlights() {
		User user = userWithId(User.create("KAKAO", "provider-user", "가효", null), 1L);
		Region region = Region.create("3", "1", "대전");
		Course firstCourse = courseWithId(
			Course.create(user, region, "대전 가족여행", LocalDate.of(2026, 6, 22), LocalDate.of(2026, 6, 23)),
			10L
		);
		setField(firstCourse, "imageUrl", "https://example.com/daejeon.jpg");
		Course secondCourse = courseWithId(
			Course.create(user, region, "부산 효도여행", LocalDate.of(2026, 7, 4), LocalDate.of(2026, 7, 6)),
			11L
		);
		setField(secondCourse, "imageUrl", "https://example.com/busan.jpg");
		Review firstReview = reviewWithId(
			Review.create(firstCourse, user, (byte) 5, "대전 여행이 좋았어요.", (byte) 10),
			100L
		);
		Review secondReview = reviewWithId(
			Review.create(secondCourse, user, (byte) 4, "부산 코스가 편했어요.", (byte) 8),
			101L
		);

		given(reviewRepository.findWrittenReviewsByUserId(1L)).willReturn(List.of(firstReview, secondReview));
		given(reviewHighlightRepository.findByReview_ReviewIdIn(List.of(100L, 101L))).willReturn(List.of(
			ReviewHighlight.create(firstReview, "여행 코스"),
			ReviewHighlight.create(firstReview, "맛집"),
			ReviewHighlight.create(secondReview, "교통")
		));

		var response = reviewService.getMyReviews(1L);

		assertThat(response.reviews()).hasSize(2);
		assertThat(response.reviews().get(0).reviewId()).isEqualTo(100L);
		assertThat(response.reviews().get(0).course().courseId()).isEqualTo(10L);
		assertThat(response.reviews().get(0).course().title()).isEqualTo("대전 가족여행");
		assertThat(response.reviews().get(0).course().period()).isEqualTo("2026. 06. 22 - 06. 23");
		assertThat(response.reviews().get(0).course().imageUrl()).isEqualTo("https://example.com/daejeon.jpg");
		assertThat(response.reviews().get(0).highlights()).containsExactly("여행 코스", "맛집");
		assertThat(response.reviews().get(1).reviewId()).isEqualTo(101L);
		assertThat(response.reviews().get(1).course().courseId()).isEqualTo(11L);
		assertThat(response.reviews().get(1).highlights()).containsExactly("교통");
	}

	@Test
	void updateMyReview_updatesReviewAndReplacesHighlights() {
		User user = userWithId(User.create("KAKAO", "provider-user", "가효", null), 1L);
		Region region = Region.create("3", "1", "대전");
		Course course = courseWithId(
			Course.create(user, region, "대전 가족여행", LocalDate.of(2026, 6, 22), LocalDate.of(2026, 6, 23)),
			10L
		);
		Review review = reviewWithId(
			Review.create(course, user, (byte) 5, "정말 만족스러웠어요.", (byte) 10),
			100L
		);
		List<ReviewHighlight> existingHighlights = List.of(
			ReviewHighlight.create(review, "여행 코스"),
			ReviewHighlight.create(review, "맛집")
		);
		UpdateReviewRequest request = new UpdateReviewRequest(
			(byte) 4,
			" 다시 생각해도 편한 여행이었어요. ",
			(byte) 8,
			List.of("편의시설")
		);

		given(reviewRepository.findByCourse_CourseIdAndUser_UserId(10L, 1L)).willReturn(Optional.of(review));
		given(reviewHighlightRepository.findByReview_ReviewId(100L)).willReturn(existingHighlights);

		var response = reviewService.updateMyReview(1L, 10L, request);

		assertThat(response.rating()).isEqualTo((byte) 4);
		assertThat(response.comment()).isEqualTo("다시 생각해도 편한 여행이었어요.");
		assertThat(response.recommendationScore()).isEqualTo((byte) 8);
		assertThat(response.highlights()).containsExactly("편의시설");
		verify(reviewHighlightRepository).deleteAll(existingHighlights);
		verify(reviewHighlightRepository).saveAll(anyList());
	}

	private User userWithId(User user, Long userId) {
		return setField(user, "userId", userId);
	}

	private Course courseWithId(Course course, Long courseId) {
		return setField(course, "courseId", courseId);
	}

	private Review reviewWithId(Review review, Long reviewId) {
		return setField(review, "reviewId", reviewId);
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
