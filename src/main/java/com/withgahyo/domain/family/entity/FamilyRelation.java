package com.withgahyo.domain.family.entity;

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
		name = "family_relation",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_family_relation_user_family_user",
				columnNames = {"user_id", "family_user_id"}
		)
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FamilyRelation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "family_relation_id")
	private Long familyRelationId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "family_user_id", nullable = false)
	private User familyUser;

	@Column(name = "relationship", nullable = false, length = 30)
	private String relationship;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@PrePersist
	void prePersist() {
		this.createdAt = LocalDateTime.now();
	}

	public static FamilyRelation create(User user, User familyUser, String relationship) {
		FamilyRelation familyRelation = new FamilyRelation();
		familyRelation.user = user;
		familyRelation.familyUser = familyUser;
		familyRelation.relationship = relationship;
		return familyRelation;
	}

	public boolean isActive() {
		return deletedAt == null;
	}

	public void restore(String relationship) {
		this.relationship = relationship;
		this.deletedAt = null;
	}
}
