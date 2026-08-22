package com.withgahyo.domain.place.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "place",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_place_content_id_content_type_id",
				columnNames = {"content_id", "content_type_id"}
		)
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "place_id")
	private Long placeId;

	@Column(name = "content_id", nullable = false, length = 50)
	private String contentId;

	@Column(name = "content_type_id", nullable = false, length = 20)
	private String contentTypeId;

	@Column(name = "cat1", length = 20)
	private String cat1;

	@Column(name = "cat2", length = 20)
	private String cat2;

	@Column(name = "cat3", length = 20)
	private String cat3;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "region_id", nullable = false)
	private Region region;

	@Column(name = "name", nullable = false, length = 255)
	private String name;

	@Column(name = "address", length = 500)
	private String address;

	@Column(name = "latitude", precision = 10, scale = 7)
	private BigDecimal latitude;

	@Column(name = "longitude", precision = 10, scale = 7)
	private BigDecimal longitude;

	@Column(name = "description", columnDefinition = "text")
	private String description;

	@Column(name = "image_url", length = 500)
	private String imageUrl;

	@Column(name = "phone_number", length = 50)
	private String phoneNumber;

	@Column(name = "source_modified_at")
	private LocalDateTime sourceModifiedAt;

	@Column(name = "last_synced_at")
	private LocalDateTime lastSyncedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	public static Place create(
		String contentId,
		String contentTypeId,
		String category,
		Region region,
		String name,
		String address,
		BigDecimal latitude,
		BigDecimal longitude,
		String imageUrl
	) {
		Place place = new Place();
		place.contentId = contentId;
		place.contentTypeId = contentTypeId;
		place.cat1 = category;
		place.region = region;
		place.name = name;
		place.address = address;
		place.latitude = latitude;
		place.longitude = longitude;
		place.imageUrl = imageUrl;
		return place;
	}

	public void updateExternalInfo(
		String category,
		Region region,
		String name,
		String address,
		BigDecimal latitude,
		BigDecimal longitude,
		String imageUrl
	) {
		this.cat1 = category;
		this.region = region;
		this.name = name;
		this.address = address;
		this.latitude = latitude;
		this.longitude = longitude;
		this.imageUrl = imageUrl;
	}
}
