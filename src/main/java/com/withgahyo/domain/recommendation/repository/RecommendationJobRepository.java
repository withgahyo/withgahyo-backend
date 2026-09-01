package com.withgahyo.domain.recommendation.repository;

import com.withgahyo.domain.recommendation.entity.RecommendationJob;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecommendationJobRepository extends JpaRepository<RecommendationJob, Long> {

	@Query("""
		select rj
		from RecommendationJob rj
		join fetch rj.course c
		join fetch c.creatorUser
		where rj.recommendationJobId = :recommendationJobId
		""")
	Optional<RecommendationJob> findWithCourseByRecommendationJobId(@Param("recommendationJobId") Long recommendationJobId);
}
