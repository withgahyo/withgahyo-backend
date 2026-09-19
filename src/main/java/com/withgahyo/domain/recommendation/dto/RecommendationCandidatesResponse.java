package com.withgahyo.domain.recommendation.dto;

import com.withgahyo.domain.course.entity.TransportMode;
import com.withgahyo.domain.recommendation.entity.RecommendationCandidate;
import com.withgahyo.domain.recommendation.entity.RecommendationJob;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

public record RecommendationCandidatesResponse(
	Long generationId,
	Long courseId,
	Long selectedCandidateId,
	List<CandidateResponse> candidates
) {

	public static RecommendationCandidatesResponse of(
		RecommendationJob job,
		List<RecommendationCandidate> candidates,
		Map<Long, String> thumbnailImageUrlByCandidateId
	) {
		Long selectedCandidateId = candidates.stream()
			.filter(RecommendationCandidate::isSelected)
			.map(RecommendationCandidate::getRecommendationCandidateId)
			.findFirst()
			.orElse(null);
		return new RecommendationCandidatesResponse(
			job.getRecommendationJobId(),
			job.getCourse().getCourseId(),
			selectedCandidateId,
			candidates.stream()
				.map(candidate -> CandidateResponse.from(
					candidate,
					thumbnailImageUrlByCandidateId.get(candidate.getRecommendationCandidateId())
				))
				.toList()
		);
	}

	public record CandidateResponse(
		Long candidateId,
		String title,
		String summary,
		BigDecimal matchScore,
		List<String> tags,
		String thumbnailImageUrl,
		BigDecimal totalDistanceKm,
		Integer estimatedTravelMinutes,
		TransportMode transportMode
	) {

		public static CandidateResponse from(RecommendationCandidate candidate, String thumbnailImageUrl) {
			return new CandidateResponse(
				candidate.getRecommendationCandidateId(),
				candidate.getTitle(),
				candidate.getDescription(),
				candidate.getFitScore(),
				List.of(),
				thumbnailImageUrl,
				toKilometers(candidate.getTotalDistanceMeters()),
				candidate.getTotalWalkingTimeMinutes(),
				null
			);
		}

		private static BigDecimal toKilometers(Integer meters) {
			if (meters == null) {
				return null;
			}
			return BigDecimal.valueOf(meters)
				.divide(BigDecimal.valueOf(1000), 1, RoundingMode.HALF_UP);
		}
	}
}
