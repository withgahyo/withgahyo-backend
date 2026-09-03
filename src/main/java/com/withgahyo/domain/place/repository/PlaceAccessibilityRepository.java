package com.withgahyo.domain.place.repository;

import com.withgahyo.domain.place.entity.AccessibilityStatus;
import com.withgahyo.domain.place.entity.PlaceAccessibility;
import com.withgahyo.domain.place.entity.PlaceAccessibilityId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceAccessibilityRepository extends JpaRepository<PlaceAccessibility, PlaceAccessibilityId> {

	@Query("""
		select pa
		from PlaceAccessibility pa
		join fetch pa.facility
		where pa.place.placeId in :placeIds
			and pa.status = :status
		order by pa.place.placeId asc, pa.facility.facilityId asc
		""")
	List<PlaceAccessibility> findAllByPlaceIdsAndStatus(
		@Param("placeIds") List<Long> placeIds,
		@Param("status") AccessibilityStatus status
	);
}
