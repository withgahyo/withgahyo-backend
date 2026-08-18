package com.withgahyo.domain.place.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
public class PlaceAccessibilityId implements Serializable {

	@Column(name = "place_id")
	private Long placeId;

	@Column(name = "facility_id")
	private Long facilityId;
}
