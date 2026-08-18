package com.withgahyo.domain.auth.entity;

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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "refresh_token")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "refresh_token_id")
	private Long refreshTokenId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "token", nullable = false, unique = true, length = 1000)
	private String token;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "revoked_at")
	private LocalDateTime revokedAt;

	public static RefreshToken create(User user, String token, LocalDateTime expiresAt) {
		RefreshToken refreshToken = new RefreshToken();
		refreshToken.user = user;
		refreshToken.token = token;
		refreshToken.expiresAt = expiresAt;
		return refreshToken;
	}

	public boolean isOwnedBy(Long userId) {
		return user.getUserId().equals(userId);
	}

	public boolean isActive(LocalDateTime now) {
		return revokedAt == null && expiresAt != null && expiresAt.isAfter(now);
	}

	public void revoke(LocalDateTime revokedAt) {
		this.revokedAt = revokedAt;
	}

	@PrePersist
	void prePersist() {
		this.createdAt = LocalDateTime.now();
	}
}
