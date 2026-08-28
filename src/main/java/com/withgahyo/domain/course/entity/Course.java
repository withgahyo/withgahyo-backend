package com.withgahyo.domain.course.entity;

import com.withgahyo.domain.course.exception.CourseErrorCode;
import com.withgahyo.domain.place.entity.Region;
import com.withgahyo.domain.user.entity.User;
import com.withgahyo.global.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "course")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "course_id")
	private Long courseId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "creator_user_id", nullable = false)
	private User creatorUser;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "region_id", nullable = false)
	private Region region;

	@Column(name = "title", nullable = false, length = 100)
	private String title;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Column(name = "start_time")
	private LocalTime startTime;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private CourseStatus status = CourseStatus.DRAFT;

	@Column(name = "image_url", length = 500)
	private String imageUrl;

	@Column(name = "confirmed_at")
	private LocalDateTime confirmedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	public static Course create(User creatorUser, Region region, String title, LocalDate startDate, LocalDate endDate) {
		Course course = new Course();
		course.creatorUser = creatorUser;
		course.region = region;
		course.title = title;
		course.startDate = startDate;
		course.endDate = endDate;
		course.status = CourseStatus.DRAFT;
		return course;
	}

	public void confirm() {
		if (confirmedAt != null || status == CourseStatus.UPCOMING) {
			throw new BusinessException(CourseErrorCode.COURSE_ALREADY_CONFIRMED);
		}
		if (status != CourseStatus.DRAFT) {
			throw new BusinessException(CourseErrorCode.COURSE_NOT_CONFIRMABLE);
		}
		this.status = CourseStatus.UPCOMING;
		this.confirmedAt = LocalDateTime.now();
	}

	public void updateBasicInfo(String title) {
		if (!isEditable()) {
			throw new BusinessException(CourseErrorCode.COURSE_NOT_EDITABLE);
		}
		this.title = title.strip();
		this.updatedAt = LocalDateTime.now();
	}

	public boolean isDeleted() {
		return deletedAt != null;
	}

	public boolean isEditable() {
		return !isDeleted() && status == CourseStatus.DRAFT;
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
