package com.withgahyo.domain.family.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "food_preference",
		uniqueConstraints = @UniqueConstraint(name = "uk_food_preference_code", columnNames = "code")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FoodPreference {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "food_preference_id")
	private Long foodPreferenceId;

	@Column(name = "code", nullable = false, length = 50)
	private String code;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "is_active", nullable = false)
	private boolean active = true;
}
