package com.withgahyo.domain.place.entity;

import com.withgahyo.domain.family.entity.Facility;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "place_accessibility")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceAccessibility {

	@EmbeddedId
	private PlaceAccessibilityId id;

	@MapsId("placeId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "place_id", nullable = false)
	private Place place;

	@MapsId("facilityId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "facility_id", nullable = false)
	private Facility facility;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private AccessibilityStatus status = AccessibilityStatus.UNKNOWN;

	@Enumerated(EnumType.STRING)
	@Column(name = "source", nullable = false, length = 30)
	private AccessibilitySource source = AccessibilitySource.TOUR_API;

	@Column(name = "verified_at")
	private LocalDateTime verifiedAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	@PreUpdate
	void updateTimestamp() {
		this.updatedAt = LocalDateTime.now();
	}
}
