package com.withgahyo.domain.user.repository;

import com.withgahyo.domain.user.entity.UserFoodPreference;
import com.withgahyo.domain.user.entity.UserFoodPreferenceId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserFoodPreferenceRepository extends JpaRepository<UserFoodPreference, UserFoodPreferenceId> {

	@Query("""
		select ufp.foodPreference.code
		from UserFoodPreference ufp
		where ufp.user.userId = :userId
		order by ufp.foodPreference.foodPreferenceId asc
		""")
	List<String> findCodesByUserId(@Param("userId") Long userId);
}
