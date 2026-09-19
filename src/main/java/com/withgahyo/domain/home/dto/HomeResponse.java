package com.withgahyo.domain.home.dto;

import com.withgahyo.domain.course.entity.Course;
import com.withgahyo.domain.recommendation.entity.RecommendationCandidate;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record HomeResponse(
	List<FamilyCourseResponse> familyCourses
) {

	public static HomeResponse empty() {
		return new HomeResponse(List.of());
	}

	public static HomeResponse of(
		List<Course> courses,
		Map<Long, List<String>> tagsByCourseId,
		Map<Long, String> imageUrlByCourseId,
		Map<Long, List<RecommendationCandidate>> alternativeCandidatesByCourseId,
		Map<Long, String> candidateThumbnailImageUrlByCandidateId
	) {
		return new HomeResponse(
			courses.stream()
				.map(course -> FamilyCourseResponse.from(
					course,
					tagsByCourseId.getOrDefault(course.getCourseId(), List.of()),
					imageUrlByCourseId.get(course.getCourseId()),
					alternativeCandidatesByCourseId.getOrDefault(course.getCourseId(), List.of()),
					candidateThumbnailImageUrlByCandidateId
				))
				.toList()
		);
	}

	public record FamilyCourseResponse(
		Long courseId,
		String title,
		String regionName,
		String imageUrl,
		LocalDate startDate,
		long daysUntilTrip,
		List<String> tags,
		List<AlternativeCandidateResponse> alternativeCandidates
	) {

		public static FamilyCourseResponse from(
			Course course,
			List<String> tags,
			String imageUrl,
			List<RecommendationCandidate> alternativeCandidates,
			Map<Long, String> candidateThumbnailImageUrlByCandidateId
		) {
			return new FamilyCourseResponse(
				course.getCourseId(),
				course.getTitle(),
				course.getRegion().getName(),
				imageUrl,
				course.getStartDate(),
				course.daysUntilTrip(),
				tags,
				alternativeCandidates.stream()
					.map(candidate -> AlternativeCandidateResponse.from(
						candidate,
						candidateThumbnailImageUrlByCandidateId.get(candidate.getRecommendationCandidateId())
					))
					.toList()
			);
		}
	}

	// 이 코스를 확정할 당시 함께 생성됐지만 선택되지 않은 RecommendationCandidate. 장소 상세 목록은
	// 포함하지 않는다(필요하면 기존 candidate 상세 API를 그대로 재사용).
	public record AlternativeCandidateResponse(
		Long candidateId,
		String title,
		String summary,
		String thumbnailImageUrl
	) {

		public static AlternativeCandidateResponse from(RecommendationCandidate candidate, String thumbnailImageUrl) {
			return new AlternativeCandidateResponse(
				candidate.getRecommendationCandidateId(),
				candidate.getTitle(),
				candidate.getDescription(),
				thumbnailImageUrl
			);
		}
	}
}
