package com.withgahyo.domain.recommendation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "recommendation_candidate",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_recommendation_candidate_job_rank",
				columnNames = {"recommendation_job_id", "rank"}
		)
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationCandidate {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "recommendation_candidate_id")
	private Long recommendationCandidateId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "recommendation_job_id", nullable = false)
	private RecommendationJob recommendationJob;

	@Column(name = "`rank`", nullable = false)
	private Integer rank;

	@Column(name = "title", nullable = false, length = 100)
	private String title;

	@Column(name = "description", length = 1000)
	private String description;

	@Column(name = "fit_score", precision = 5, scale = 2)
	private BigDecimal fitScore;

	@Column(name = "total_distance_meters")
	private Integer totalDistanceMeters;

	@Column(name = "total_walking_time_minutes")
	private Integer totalWalkingTimeMinutes;

	@Column(name = "selected", nullable = false)
	private boolean selected;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public static RecommendationCandidate create(
		RecommendationJob recommendationJob,
		Integer rank,
		String title,
		String description,
		BigDecimal fitScore,
		Integer totalDistanceMeters,
		Integer totalWalkingTimeMinutes
	) {
		RecommendationCandidate candidate = new RecommendationCandidate();
		candidate.recommendationJob = recommendationJob;
		candidate.rank = rank;
		candidate.title = title;
		candidate.description = description;
		candidate.fitScore = fitScore;
		candidate.totalDistanceMeters = totalDistanceMeters;
		candidate.totalWalkingTimeMinutes = totalWalkingTimeMinutes;
		candidate.selected = false;
		return candidate;
	}

	public void select() {
		this.selected = true;
	}

	@PrePersist
	void prePersist() {
		this.createdAt = LocalDateTime.now();
	}
}
