package com.withgahyo.domain.review.repository;

import com.withgahyo.domain.review.entity.Review;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

	boolean existsByCourse_CourseIdAndUser_UserId(Long courseId, Long userId);

	Optional<Review> findByCourse_CourseIdAndUser_UserId(Long courseId, Long userId);
}
