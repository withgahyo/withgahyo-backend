package com.withgahyo.domain.family.repository;

import com.withgahyo.domain.family.entity.TourismPreference;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TourismPreferenceRepository extends JpaRepository<TourismPreference, Long> {

	List<TourismPreference> findAllByActiveTrueOrderByTourismPreferenceIdAsc();
}
