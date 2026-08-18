package com.withgahyo.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
public class UserFacilityPreferenceId implements Serializable {

	@Column(name = "user_id")
	private Long userId;

	@Column(name = "facility_id")
	private Long facilityId;
}
