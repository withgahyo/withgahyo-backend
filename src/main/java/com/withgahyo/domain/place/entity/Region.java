package com.withgahyo.domain.place.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "region",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_region_area_code_sigungu_code",
				columnNames = {"area_code", "sigungu_code"}
		)
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Region {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "region_id")
	private Long regionId;

	@Column(name = "area_code", nullable = false, length = 20)
	private String areaCode;

	@Column(name = "sigungu_code", nullable = false, length = 20)
	private String sigunguCode;

	@Column(name = "name", nullable = false, length = 100)
	private String name;
}
