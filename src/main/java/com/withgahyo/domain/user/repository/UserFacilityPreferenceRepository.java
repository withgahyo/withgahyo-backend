package com.withgahyo.domain.user.repository;

import com.withgahyo.domain.user.entity.UserFacilityPreference;
import com.withgahyo.domain.user.entity.UserFacilityPreferenceId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserFacilityPreferenceRepository extends JpaRepository<UserFacilityPreference, UserFacilityPreferenceId> {

	@Query("""
		select ufp.facility.code
		from UserFacilityPreference ufp
		where ufp.user.userId = :userId
		order by ufp.facility.facilityId asc
		""")
	List<String> findCodesByUserId(@Param("userId") Long userId);
}
