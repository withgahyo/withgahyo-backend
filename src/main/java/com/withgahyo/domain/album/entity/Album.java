package com.withgahyo.domain.album.entity;

import com.withgahyo.domain.course.entity.Course;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "album")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Album {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "album_id")
	private Long albumId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "course_id", nullable = false)
	private Course course;

	@Column(name = "title", nullable = false, length = 100)
	private String title;

	@Column(name = "description", length = 500)
	private String description;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cover_photo_id")
	private Photo coverPhoto;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	public static Album create(Course course) {
		Album album = new Album();
		album.course = course;
		album.title = course.getTitle();
		return album;
	}

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
}
