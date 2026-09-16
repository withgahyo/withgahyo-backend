package com.withgahyo.domain.community.entity;

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
	name = "community_user_block",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_community_user_block_blocker_blocked",
		columnNames = {"blocker_user_id", "blocked_user_id"}
	)
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityUserBlock {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "block_id")
	private Long blockId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "blocker_user_id", nullable = false)
	private User blocker;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "blocked_user_id", nullable = false)
	private User blockedUser;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public static CommunityUserBlock create(User blocker, User blockedUser) {
		CommunityUserBlock block = new CommunityUserBlock();
		block.blocker = blocker;
		block.blockedUser = blockedUser;
		return block;
	}

	@PrePersist
	void prePersist() {
		this.createdAt = LocalDateTime.now();
	}
}
