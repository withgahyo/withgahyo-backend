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
public class UserTourismPreferenceId implements Serializable {

	@Column(name = "user_id")
	private Long userId;

	@Column(name = "tourism_preference_id")
	private Long tourismPreferenceId;

	public UserTourismPreferenceId(Long userId, Long tourismPreferenceId) {
		this.userId = userId;
		this.tourismPreferenceId = tourismPreferenceId;
	}
}
