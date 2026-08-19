package com.withgahyo.domain.family.repository;

import com.withgahyo.domain.family.entity.FoodPreference;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodPreferenceRepository extends JpaRepository<FoodPreference, Long> {

	List<FoodPreference> findAllByActiveTrueOrderByFoodPreferenceIdAsc();
}
