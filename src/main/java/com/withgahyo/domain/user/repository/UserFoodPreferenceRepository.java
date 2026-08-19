package com.withgahyo.domain.user.repository;

import com.withgahyo.domain.user.entity.UserFoodPreference;
import com.withgahyo.domain.user.entity.UserFoodPreferenceId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFoodPreferenceRepository extends JpaRepository<UserFoodPreference, UserFoodPreferenceId> {

	List<UserFoodPreference> findAllById_UserId(Long userId);

	boolean existsById_UserId(Long userId);

	void deleteAllById_UserId(Long userId);
}
