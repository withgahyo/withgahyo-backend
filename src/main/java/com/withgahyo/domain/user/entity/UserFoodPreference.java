package com.withgahyo.domain.user.entity;

import com.withgahyo.domain.family.entity.FoodPreference;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "user_food_preference")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserFoodPreference {

	@EmbeddedId
	private UserFoodPreferenceId id;

	@MapsId("userId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@MapsId("foodPreferenceId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "food_preference_id", nullable = false)
	private FoodPreference foodPreference;

	public static UserFoodPreference create(User user, FoodPreference foodPreference) {
		UserFoodPreference preference = new UserFoodPreference();
		preference.id = new UserFoodPreferenceId(user.getUserId(), foodPreference.getFoodPreferenceId());
		preference.user = user;
		preference.foodPreference = foodPreference;
		return preference;
	}
}
