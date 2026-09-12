package com.withgahyo.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "users",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_users_provider_provider_user_id",
				columnNames = {"provider", "provider_user_id"}
		)
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private Long userId;

	@Column(name = "provider", nullable = false, length = 20)
	private String provider;

	@Column(name = "provider_user_id", nullable = false, length = 100)
	private String providerUserId;

	@Column(name = "email", length = 255)
	private String email;

	@Column(name = "nickname", nullable = false, length = 50)
	private String nickname;

	@Lob
	@Column(name = "profile_image_url", columnDefinition = "TEXT")
	private String profileImageUrl;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	public static User create(String provider, String providerUserId, String nickname, String profileImageUrl) {
		return create(provider, providerUserId, null, nickname, profileImageUrl);
	}

	public static User create(
		String provider,
		String providerUserId,
		String email,
		String nickname,
		String profileImageUrl
	) {
		User user = new User();
		user.provider = provider;
		user.providerUserId = providerUserId;
		user.email = email;
		user.nickname = nickname;
		user.profileImageUrl = profileImageUrl;
		return user;
	}

	public void restoreOrUpdate(String email, String nickname, String profileImageUrl) {
		this.deletedAt = null;
		this.email = email;
		this.nickname = nickname;
		this.profileImageUrl = profileImageUrl;
	}

	public void updateOAuthEmail(String email) {
		this.email = email;
	}

	public void withdraw(LocalDateTime withdrawnAt) {
		this.deletedAt = withdrawnAt;
	}

	public void updateProfile(String nickname, String profileImageUrl) {
		if (nickname != null) {
			this.nickname = nickname;
		}
		if (profileImageUrl != null) {
			this.profileImageUrl = profileImageUrl;
		}
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
