package com.withgahyo.domain.user.repository;

import com.withgahyo.domain.user.entity.UserTourismPreference;
import com.withgahyo.domain.user.entity.UserTourismPreferenceId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTourismPreferenceRepository extends JpaRepository<UserTourismPreference, UserTourismPreferenceId> {

	List<UserTourismPreference> findAllById_UserId(Long userId);

	boolean existsById_UserId(Long userId);

	void deleteAllById_UserId(Long userId);
}
