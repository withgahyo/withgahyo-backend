package com.withgahyo.domain.recommendation.repository;

import com.withgahyo.domain.recommendation.entity.RecommendationCandidate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecommendationCandidateRepository extends JpaRepository<RecommendationCandidate, Long> {

	List<RecommendationCandidate> findAllByRecommendationJobRecommendationJobIdOrderByRankAsc(Long recommendationJobId);

	Optional<RecommendationCandidate> findByRecommendationCandidateIdAndRecommendationJobRecommendationJobId(
		Long recommendationCandidateId,
		Long recommendationJobId
	);

	@Query("""
		select rc
		from RecommendationCandidate rc
		where rc.recommendationJob.recommendationJobId = :recommendationJobId
			and rc.selected = true
		""")
	Optional<RecommendationCandidate> findSelectedByRecommendationJobId(@Param("recommendationJobId") Long recommendationJobId);
}
