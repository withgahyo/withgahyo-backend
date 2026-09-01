package com.withgahyo.domain.recommendation.repository;

import com.withgahyo.domain.recommendation.entity.RecommendationCandidateItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationCandidateItemRepository extends JpaRepository<RecommendationCandidateItem, Long> {

	List<RecommendationCandidateItem> findAllByRecommendationCandidateRecommendationCandidateIdOrderByDayNumberAscVisitOrderAsc(
		Long recommendationCandidateId
	);
}
