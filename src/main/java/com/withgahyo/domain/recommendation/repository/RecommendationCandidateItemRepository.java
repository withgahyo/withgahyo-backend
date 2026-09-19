package com.withgahyo.domain.recommendation.repository;

import com.withgahyo.domain.recommendation.entity.RecommendationCandidateItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecommendationCandidateItemRepository extends JpaRepository<RecommendationCandidateItem, Long> {

	List<RecommendationCandidateItem> findAllByRecommendationCandidateRecommendationCandidateIdOrderByDayNumberAscVisitOrderAsc(
		Long recommendationCandidateId
	);

	/**
	 * 여러 후보의 방문 항목을 여행 순서(dayNumber, visitOrder)대로 한 번에 조회한다.
	 * 후보 대표 이미지 계산에 place.imageUrl 과 소속 후보 식별자가 필요하므로 두 연관을 함께 fetch 해
	 * 후보/장소 개수와 무관하게 쿼리 한 번으로 끝낸다.
	 */
	@Query("""
		select rci
		from RecommendationCandidateItem rci
		join fetch rci.place
		join fetch rci.recommendationCandidate
		where rci.recommendationCandidate.recommendationCandidateId in :recommendationCandidateIds
		order by rci.dayNumber asc, rci.visitOrder asc
		""")
	List<RecommendationCandidateItem> findAllWithPlaceByRecommendationCandidateIdIn(
		@Param("recommendationCandidateIds") List<Long> recommendationCandidateIds
	);
}
