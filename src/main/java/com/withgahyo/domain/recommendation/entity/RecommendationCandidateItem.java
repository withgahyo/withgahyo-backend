package com.withgahyo.domain.recommendation.entity;

import com.withgahyo.domain.course.entity.TransportMode;
import com.withgahyo.domain.place.entity.Place;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "recommendation_candidate_item",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_recommendation_candidate_item_order",
				columnNames = {"recommendation_candidate_id", "day_number", "visit_order"}
		)
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationCandidateItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "candidate_item_id")
	private Long candidateItemId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "recommendation_candidate_id", nullable = false)
	private RecommendationCandidate recommendationCandidate;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "place_id", nullable = false)
	private Place place;

	@Column(name = "day_number", nullable = false)
	private Integer dayNumber;

	@Column(name = "visit_order", nullable = false)
	private Integer visitOrder;

	@Column(name = "arrival_time")
	private LocalTime arrivalTime;

	@Column(name = "departure_time")
	private LocalTime departureTime;

	@Enumerated(EnumType.STRING)
	@Column(name = "transport_mode_to_next", length = 30)
	private TransportMode transportModeToNext;

	@Column(name = "duration_minutes_to_next")
	private Integer durationMinutesToNext;

	@Column(name = "distance_meters_to_next")
	private Integer distanceMetersToNext;

	public static RecommendationCandidateItem create(
		RecommendationCandidate recommendationCandidate,
		Place place,
		Integer dayNumber,
		Integer visitOrder,
		LocalTime arrivalTime,
		LocalTime departureTime,
		TransportMode transportModeToNext,
		Integer durationMinutesToNext,
		Integer distanceMetersToNext
	) {
		RecommendationCandidateItem item = new RecommendationCandidateItem();
		item.recommendationCandidate = recommendationCandidate;
		item.place = place;
		item.dayNumber = dayNumber;
		item.visitOrder = visitOrder;
		item.arrivalTime = arrivalTime;
		item.departureTime = departureTime;
		item.transportModeToNext = transportModeToNext;
		item.durationMinutesToNext = durationMinutesToNext;
		item.distanceMetersToNext = distanceMetersToNext;
		return item;
	}
}
