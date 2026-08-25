package com.withgahyo.domain.place.repository;

import com.withgahyo.domain.place.entity.Place;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Long> {

	Optional<Place> findByContentIdAndContentTypeId(String contentId, String contentTypeId);

	List<Place> findAllByPlaceIdIn(List<Long> placeIds);
}
