package com.withgahyo.domain.place.repository;

import com.withgahyo.domain.place.entity.Region;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<Region, Long> {

	Optional<Region> findByAreaCodeAndSigunguCode(String areaCode, String sigunguCode);

	List<Region> findByNameContaining(String query);
}
