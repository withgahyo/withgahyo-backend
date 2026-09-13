package com.withgahyo.domain.review.service;

import com.withgahyo.domain.course.entity.Course;
import com.withgahyo.domain.course.entity.CourseStatus;
import com.withgahyo.domain.course.exception.CourseErrorCode;
import com.withgahyo.domain.course.repository.CourseRepository;
import com.withgahyo.domain.review.dto.CreateReviewRequest;
import com.withgahyo.domain.review.dto.PendingReviewListResponse;
import com.withgahyo.domain.review.dto.ReviewFormResponse;
import com.withgahyo.domain.review.dto.ReviewResponse;
import com.withgahyo.domain.review.entity.Review;
import com.withgahyo.domain.review.entity.ReviewHighlight;
import com.withgahyo.domain.review.exception.ReviewErrorCode;
import com.withgahyo.domain.review.repository.ReviewHighlightRepository;
import com.withgahyo.domain.review.repository.ReviewRepository;
import com.withgahyo.global.exception.BusinessException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReviewService {

	private final CourseRepository courseRepository;
	private final ReviewRepository reviewRepository;
	private final ReviewHighlightRepository reviewHighlightRepository;

	public ReviewService(
		CourseRepository courseRepository,
		ReviewRepository reviewRepository,
		ReviewHighlightRepository reviewHighlightRepository
	) {
		this.courseRepository = courseRepository;
		this.reviewRepository = reviewRepository;
		this.reviewHighlightRepository = reviewHighlightRepository;
	}

	public PendingReviewListResponse getPendingReviews(Long userId) {
		return PendingReviewListResponse.from(courseRepository.findReviewPendingCourses(userId));
	}

	public ReviewFormResponse getReviewForm(Long userId, Long courseId) {
		return ReviewFormResponse.from(getOwnedCourse(userId, courseId));
	}

	@Transactional
	public ReviewResponse createReview(Long userId, Long courseId, CreateReviewRequest request) {
		Course course = getOwnedCourse(userId, courseId);
		if (course.getStatus() != CourseStatus.COMPLETED) {
			throw new BusinessException(ReviewErrorCode.COURSE_NOT_REVIEWABLE);
		}
		if (reviewRepository.existsByCourse_CourseIdAndUser_UserId(courseId, userId)) {
			throw new BusinessException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
		}

		Review review = reviewRepository.save(
			Review.create(course, course.getCreatorUser(), request.rating(), request.comment(), request.recommendationScore())
		);
		List<String> highlights = request.normalizedHighlights();
		List<ReviewHighlight> reviewHighlights = highlights.stream()
			.map(highlight -> ReviewHighlight.create(review, highlight))
			.toList();
		reviewHighlightRepository.saveAll(reviewHighlights);
		return ReviewResponse.of(review, highlights);
	}

	private Course getOwnedCourse(Long userId, Long courseId) {
		Course course = courseRepository.findActiveById(courseId)
			.orElseThrow(() -> new BusinessException(CourseErrorCode.COURSE_NOT_FOUND));
		if (!course.getCreatorUser().getUserId().equals(userId)) {
			throw new BusinessException(CourseErrorCode.COURSE_ACCESS_DENIED);
		}
		return course;
	}
}
