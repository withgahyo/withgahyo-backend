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
public class UserFoodPreferenceId implements Serializable {

	@Column(name = "user_id")
	private Long userId;

	@Column(name = "food_preference_id")
	private Long foodPreferenceId;
}
