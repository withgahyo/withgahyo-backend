package com.withgahyo.domain.recommendation.dto;

import com.withgahyo.domain.course.entity.TransportMode;
import com.withgahyo.domain.place.entity.AccessibilityStatus;
import com.withgahyo.domain.place.entity.Place;
import com.withgahyo.domain.recommendation.entity.RecommendationCandidate;
import com.withgahyo.domain.recommendation.entity.RecommendationCandidateItem;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record RecommendationCandidateDetailResponse(
	Long generationId,
	Long candidateId,
	String title,
	String summary,
	BigDecimal matchScore,
	List<String> tags,
	List<String> recommendationReasons,
	List<String> accessibilityHighlights,
	List<DayResponse> days
) {

	public static RecommendationCandidateDetailResponse of(
		RecommendationCandidate candidate,
		List<RecommendationCandidateItem> items
	) {
		LocalDate startDate = candidate.getRecommendationJob().getCourse().getStartDate();
		return new RecommendationCandidateDetailResponse(
			candidate.getRecommendationJob().getRecommendationJobId(),
			candidate.getRecommendationCandidateId(),
			candidate.getTitle(),
			candidate.getDescription(),
			candidate.getFitScore(),
			List.of(),
			List.of(),
			List.of(),
			toDays(startDate, items)
		);
	}

	private static List<DayResponse> toDays(LocalDate startDate, List<RecommendationCandidateItem> items) {
		return items.stream()
			.collect(Collectors.groupingBy(RecommendationCandidateItem::getDayNumber))
			.entrySet()
			.stream()
			.sorted(Map.Entry.comparingByKey())
			.map(entry -> new DayResponse(
				entry.getKey(),
				startDate.plusDays(entry.getKey() - 1L),
				entry.getValue()
					.stream()
					.sorted(Comparator.comparing(RecommendationCandidateItem::getVisitOrder))
					.map(PlaceResponse::from)
					.toList()
			))
			.toList();
	}

	public record DayResponse(
		Integer day,
		LocalDate date,
		List<PlaceResponse> places
	) {
	}

	public record PlaceResponse(
		Integer order,
		Long placeId,
		String source,
		String name,
		String category,
		LocalTime arrivalTime,
		LocalTime departureTime,
		BigDecimal latitude,
		BigDecimal longitude,
		String recommendationReason,
		Map<String, AccessibilityStatus> accessibility,
		TransportToNextResponse transportToNext
	) {

		public static PlaceResponse from(RecommendationCandidateItem item) {
			Place place = item.getPlace();
			return new PlaceResponse(
				item.getVisitOrder(),
				place.getPlaceId(),
				place.getSource(),
				place.getName(),
				place.getCat1(),
				item.getArrivalTime(),
				item.getDepartureTime(),
				place.getLatitude(),
				place.getLongitude(),
				null,
				Map.of(),
				new TransportToNextResponse(
					item.getTransportModeToNext(),
					item.getDurationMinutesToNext(),
					item.getDistanceMetersToNext()
				)
			);
		}
	}

	public record TransportToNextResponse(
		TransportMode mode,
		Integer durationMinutes,
		Integer distanceMeters
	) {
	}
}
