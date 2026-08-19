package com.withgahyo.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "user_onboarding_profile")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserOnboardingProfile {

	@Id
	@Column(name = "user_id")
	private Long userId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "trip_duration", length = 30)
	private String tripDuration;

	@Column(name = "walking_tolerance", length = 30)
	private String walkingTolerance;

	@Column(name = "rest_preference", length = 30)
	private String restPreference;

	@Column(name = "stairs_preference", length = 30)
	private String stairsPreference;

	@Column(name = "slope_preference", length = 30)
	private String slopePreference;

	@Column(name = "spicy_preference", length = 30)
	private String spicyPreference;

	@Column(name = "onboarding_completed", nullable = false)
	private boolean onboardingCompleted;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	@PreUpdate
	void updateTimestamp() {
		this.updatedAt = LocalDateTime.now();
	}

	public static UserOnboardingProfile create(User user) {
		UserOnboardingProfile profile = new UserOnboardingProfile();
		profile.user = user;
		profile.userId = user.getUserId();
		profile.onboardingCompleted = false;
		return profile;
	}

	// TODO: 여행 기간 허용값이 확정되면 String 대신 enum 타입으로 전환
	public void updateTripDuration(String tripDuration) {
		this.tripDuration = tripDuration;
	}

	// TODO: 컨디션 항목별 허용값이 확정되면 String 대신 enum 타입으로 전환
	public void updateConditions(
		String walkingTolerance,
		String restPreference,
		String stairsPreference,
		String slopePreference,
		String spicyPreference
	) {
		this.walkingTolerance = walkingTolerance;
		this.restPreference = restPreference;
		this.stairsPreference = stairsPreference;
		this.slopePreference = slopePreference;
		this.spicyPreference = spicyPreference;
	}

	public void updateProfile(
		String tripDuration,
		String walkingTolerance,
		String restPreference,
		String stairsPreference,
		String slopePreference,
		String spicyPreference
	) {
		updateTripDuration(tripDuration);
		updateConditions(walkingTolerance, restPreference, stairsPreference, slopePreference, spicyPreference);
	}

	public void complete() {
		this.onboardingCompleted = true;
	}

	public boolean isProfileFilled() {
		return isNotBlank(tripDuration)
			&& isNotBlank(walkingTolerance)
			&& isNotBlank(restPreference)
			&& isNotBlank(stairsPreference)
			&& isNotBlank(slopePreference)
			&& isNotBlank(spicyPreference);
	}

	private boolean isNotBlank(String value) {
		return value != null && !value.isBlank();
	}
}
