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
		// userId는 @MapsId를 통해 persist 시점에 user의 식별자에서 파생된다.
		// 여기서 직접 세팅하면 Spring Data의 isNew() 판정이 어긋나 save()가 persist() 대신 merge()를 타므로 설정하지 않는다.
		UserOnboardingProfile profile = new UserOnboardingProfile();
		profile.user = user;
		profile.onboardingCompleted = false;
		return profile;
	}

	// TODO: 컨디션 항목별 허용값이 확정되면 String 대신 enum 타입으로 전환. 현재는 프론트가 화면 label이 아닌 option id 문자열을 전송함
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

	public void complete() {
		this.onboardingCompleted = true;
	}
}
