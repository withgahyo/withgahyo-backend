package com.withgahyo.domain.course.entity;

import com.withgahyo.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "course_participant",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_course_participant_course_user",
				columnNames = {"course_id", "user_id"}
		)
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseParticipant {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "course_participant_id")
	private Long courseParticipantId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "course_id", nullable = false)
	private Course course;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "name_snapshot", nullable = false, length = 20)
	private String nameSnapshot;

	@Column(name = "relationship_snapshot", nullable = false, length = 30)
	private String relationshipSnapshot;

	@Column(name = "profile_image_url_snapshot", length = 500)
	private String profileImageUrlSnapshot;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void prePersist() {
		this.createdAt = LocalDateTime.now();
	}

	public static CourseParticipant create(Course course, User user, String relationshipSnapshot) {
		CourseParticipant participant = new CourseParticipant();
		participant.course = course;
		participant.user = user;
		participant.nameSnapshot = user.getNickname();
		participant.relationshipSnapshot = relationshipSnapshot;
		participant.profileImageUrlSnapshot = user.getProfileImageUrl();
		return participant;
	}
}
