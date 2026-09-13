package com.withgahyo.domain.community.entity;

import com.withgahyo.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "community_post_report")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPostReport {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "report_id")
	private Long reportId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "post_id", nullable = false)
	private CommunityPost post;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reporter_user_id", nullable = false)
	private User reporter;

	@Column(name = "reason", nullable = false, length = 30)
	private String reason;

	@Lob
	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public static CommunityPostReport create(
		CommunityPost post,
		User reporter,
		String reason,
		String description
	) {
		CommunityPostReport report = new CommunityPostReport();
		report.post = post;
		report.reporter = reporter;
		report.reason = reason;
		report.description = description;
		return report;
	}

	@PrePersist
	void prePersist() {
		this.createdAt = LocalDateTime.now();
	}
}
