package com.withgahyo.domain.user.repository;

import com.withgahyo.domain.user.entity.UserTourismPreference;
import com.withgahyo.domain.user.entity.UserTourismPreferenceId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserTourismPreferenceRepository extends JpaRepository<UserTourismPreference, UserTourismPreferenceId> {

	List<UserTourismPreference> findAllById_UserId(Long userId);

	boolean existsById_UserId(Long userId);

	void deleteAllById_UserId(Long userId);

	@Query("""
		select utp.tourismPreference.code
		from UserTourismPreference utp
		where utp.user.userId = :userId
		order by utp.tourismPreference.tourismPreferenceId asc
		""")
	List<String> findCodesByUserId(@Param("userId") Long userId);
}
