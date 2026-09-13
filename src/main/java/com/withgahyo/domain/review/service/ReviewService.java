package com.withgahyo.domain.review.service;

import com.withgahyo.domain.course.repository.CourseRepository;
import com.withgahyo.domain.review.dto.PendingReviewListResponse;
import com.withgahyo.domain.review.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReviewService {

	private final CourseRepository courseRepository;
	private final ReviewRepository reviewRepository;

	public ReviewService(CourseRepository courseRepository, ReviewRepository reviewRepository) {
		this.courseRepository = courseRepository;
		this.reviewRepository = reviewRepository;
	}

	public PendingReviewListResponse getPendingReviews(Long userId) {
		return PendingReviewListResponse.from(courseRepository.findReviewPendingCourses(userId));
	}
}
