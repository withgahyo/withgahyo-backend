package com.withgahyo.domain.review.service;

import com.withgahyo.domain.community.entity.CommunityPost;
import com.withgahyo.domain.community.repository.CommunityPostRepository;
import com.withgahyo.domain.course.entity.Course;
import com.withgahyo.domain.course.entity.CourseStatus;
import com.withgahyo.domain.course.exception.CourseErrorCode;
import com.withgahyo.domain.course.repository.CourseParticipantRepository;
import com.withgahyo.domain.course.repository.CourseRepository;
import com.withgahyo.domain.review.dto.CreateReviewRequest;
import com.withgahyo.domain.review.dto.MyReviewListResponse;
import com.withgahyo.domain.review.dto.PendingReviewListResponse;
import com.withgahyo.domain.review.dto.ReviewFormResponse;
import com.withgahyo.domain.review.dto.ReviewResponse;
import com.withgahyo.domain.review.dto.UpdateReviewRequest;
import com.withgahyo.domain.review.entity.Review;
import com.withgahyo.domain.review.entity.ReviewHighlight;
import com.withgahyo.domain.review.exception.ReviewErrorCode;
import com.withgahyo.domain.review.repository.ReviewHighlightRepository;
import com.withgahyo.domain.review.repository.ReviewRepository;
import com.withgahyo.domain.user.entity.User;
import com.withgahyo.domain.user.repository.UserRepository;
import com.withgahyo.global.exception.BusinessException;
import com.withgahyo.global.exception.code.SecurityErrorCode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReviewService {

	private final CourseRepository courseRepository;
	private final CourseParticipantRepository courseParticipantRepository;
	private final ReviewRepository reviewRepository;
	private final ReviewHighlightRepository reviewHighlightRepository;
	private final CommunityPostRepository communityPostRepository;
	private final UserRepository userRepository;

	public ReviewService(
		CourseRepository courseRepository,
		CourseParticipantRepository courseParticipantRepository,
		ReviewRepository reviewRepository,
		ReviewHighlightRepository reviewHighlightRepository,
		CommunityPostRepository communityPostRepository,
		UserRepository userRepository
	) {
		this.courseRepository = courseRepository;
		this.courseParticipantRepository = courseParticipantRepository;
		this.reviewRepository = reviewRepository;
		this.reviewHighlightRepository = reviewHighlightRepository;
		this.communityPostRepository = communityPostRepository;
		this.userRepository = userRepository;
	}

	public PendingReviewListResponse getPendingReviews(Long userId) {
		return PendingReviewListResponse.from(courseRepository.findReviewPendingCourses(userId));
	}

	public ReviewFormResponse getReviewForm(Long userId, Long courseId) {
		return ReviewFormResponse.from(getAccessibleCourse(userId, courseId));
	}

	public ReviewResponse getMyReview(Long userId, Long courseId) {
		Review review = getReview(userId, courseId);
		return ReviewResponse.of(review, getHighlights(review));
	}

	public MyReviewListResponse getMyReviews(Long userId) {
		List<Review> reviews = reviewRepository.findWrittenReviewsByUserId(userId);
		List<Long> reviewIds = reviews.stream()
			.map(Review::getReviewId)
			.toList();
		Map<Long, List<String>> highlightsByReviewId = reviewIds.isEmpty()
			? Map.of()
			: reviewHighlightRepository.findByReview_ReviewIdIn(reviewIds).stream()
				.collect(Collectors.groupingBy(
					highlight -> highlight.getReview().getReviewId(),
					Collectors.mapping(ReviewHighlight::getHighlightType, Collectors.toList())
				));
		return MyReviewListResponse.of(reviews, highlightsByReviewId);
	}

	@Transactional
	public ReviewResponse updateMyReview(Long userId, Long courseId, UpdateReviewRequest request) {
		Review review = getReview(userId, courseId);
		review.update(request.rating(), request.comment(), request.recommendationScore());
		reviewHighlightRepository.deleteAll(reviewHighlightRepository.findByReview_ReviewId(review.getReviewId()));

		List<String> highlights = request.normalizedHighlights();
		reviewHighlightRepository.saveAll(
			highlights.stream()
				.map(highlight -> ReviewHighlight.create(review, highlight))
				.toList()
		);
		return ReviewResponse.of(review, highlights);
	}

	@Transactional
	public ReviewResponse createReview(Long userId, Long courseId, CreateReviewRequest request) {
		Course course = getAccessibleCourse(userId, courseId);
		if (!isReviewable(course)) {
			throw new BusinessException(ReviewErrorCode.COURSE_NOT_REVIEWABLE);
		}
		if (reviewRepository.existsByCourse_CourseIdAndUser_UserId(courseId, userId)) {
			throw new BusinessException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
		}
		User reviewer = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(SecurityErrorCode.INVALID_TOKEN));

		Review review = reviewRepository.save(
			Review.create(course, reviewer, request.rating(), request.comment(), request.recommendationScore())
		);
		List<String> highlights = request.normalizedHighlights();
		List<ReviewHighlight> reviewHighlights = highlights.stream()
			.map(highlight -> ReviewHighlight.create(review, highlight))
			.toList();
		reviewHighlightRepository.saveAll(reviewHighlights);
		communityPostRepository.save(CommunityPost.create(review));
		return ReviewResponse.of(review, highlights);
	}

	private Course getAccessibleCourse(Long userId, Long courseId) {
		Course course = courseRepository.findActiveById(courseId)
			.orElseThrow(() -> new BusinessException(CourseErrorCode.COURSE_NOT_FOUND));
		if (course.getCreatorUser().getUserId().equals(userId)) {
			return course;
		}
		if (courseParticipantRepository.existsByCourseIdAndUserId(courseId, userId)) {
			return course;
		}
		throw new BusinessException(CourseErrorCode.COURSE_ACCESS_DENIED);
	}

	private boolean isReviewable(Course course) {
		return course.getStatus() == CourseStatus.COMPLETED
			|| (course.getStatus() == CourseStatus.UPCOMING && course.getEndDate().isBefore(LocalDate.now()));
	}

	private Review getReview(Long userId, Long courseId) {
		return reviewRepository.findByCourse_CourseIdAndUser_UserId(courseId, userId)
			.orElseThrow(() -> new BusinessException(ReviewErrorCode.REVIEW_NOT_FOUND));
	}

	private List<String> getHighlights(Review review) {
		return reviewHighlightRepository.findByReview_ReviewId(review.getReviewId()).stream()
			.map(ReviewHighlight::getHighlightType)
			.toList();
	}
}
