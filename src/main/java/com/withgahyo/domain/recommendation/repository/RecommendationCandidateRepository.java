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

	/**
	 * Home의 alternativeCandidates용. courseIds에 속한 Course들이 지금까지 만든 모든
	 * RecommendationJob(재시도로 여러 개일 수 있음)의 candidate를 한 번에 조회한다.
	 * job 단위로 "확정에 쓰인 job"(selected=true 후보가 속한 job)을 가려내는 일은 Service가 담당하고,
	 * 여기서는 그 판별에 필요한 recommendationJob을 함께 fetch 해 N+1 없이 candidate/job을 로딩한다.
	 * rank asc 정렬은 기존 findAllByRecommendationJobRecommendationJobIdOrderByRankAsc와 동일한 정책이다.
	 */
	@Query("""
		select rc
		from RecommendationCandidate rc
		join fetch rc.recommendationJob rj
		where rj.course.courseId in :courseIds
		order by rj.course.courseId asc, rj.recommendationJobId asc, rc.rank asc
		""")
	List<RecommendationCandidate> findAllByJobCourseIdIn(@Param("courseIds") List<Long> courseIds);
}
