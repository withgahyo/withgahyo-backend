package com.withgahyo.domain.review.repository;

import com.withgahyo.domain.review.entity.Review;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

	boolean existsByCourse_CourseIdAndUser_UserId(Long courseId, Long userId);

	Optional<Review> findByCourse_CourseIdAndUser_UserId(Long courseId, Long userId);

	@Query("""
		select r
		from Review r
		join fetch r.course c
		join fetch c.region
		where r.user.userId = :userId
			and c.deletedAt is null
		order by r.createdAt desc, r.reviewId desc
	""")
	List<Review> findWrittenReviewsByUserId(@Param("userId") Long userId);
}
