package com.withgahyo.domain.course.entity;

import com.withgahyo.domain.place.entity.Place;
import com.withgahyo.domain.recommendation.entity.RecommendationCandidateItem;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "course_schedule_item",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_course_schedule_order",
				columnNames = {"course_id", "day_number", "visit_order"}
		)
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseScheduleItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "schedule_item_id")
	private Long scheduleItemId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "course_id", nullable = false)
	private Course course;

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

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	public static CourseScheduleItem create(
		Course course,
		Place place,
		Integer dayNumber,
		Integer visitOrder,
		LocalTime arrivalTime,
		LocalTime departureTime,
		TransportMode transportModeToNext,
		Integer durationMinutesToNext,
		Integer distanceMetersToNext
	) {
		CourseScheduleItem item = new CourseScheduleItem();
		item.course = course;
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

	public static CourseScheduleItem from(Course course, RecommendationCandidateItem candidateItem) {
		return create(
			course,
			candidateItem.getPlace(),
			candidateItem.getDayNumber(),
			candidateItem.getVisitOrder(),
			candidateItem.getArrivalTime(),
			candidateItem.getDepartureTime(),
			candidateItem.getTransportModeToNext(),
			candidateItem.getDurationMinutesToNext(),
			candidateItem.getDistanceMetersToNext()
		);
	}

	@PrePersist
	void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		this.updatedAt = LocalDateTime.now();
	}
}
