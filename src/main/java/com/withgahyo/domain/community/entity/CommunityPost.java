package com.withgahyo.domain.community.entity;

import com.withgahyo.domain.review.entity.Review;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
@Table(name = "community_post")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPost {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "post_id")
	private Long postId;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "review_id", nullable = false, unique = true)
	private Review review;

	@Column(name = "author_user_id", nullable = false)
	private Long authorUserId;

	@Column(name = "category", nullable = false, length = 30)
	private String category;

	@Column(name = "title", nullable = false, length = 100)
	private String title;

	@Column(name = "content", nullable = false, columnDefinition = "text")
	private String content;

	@Column(name = "like_count", nullable = false)
	private int likeCount;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	public static CommunityPost create(Review review) {
		CommunityPost post = new CommunityPost();
		post.review = review;
		post.authorUserId = review.getUser().getUserId();
		post.category = "REVIEW";
		post.title = review.getCourse().getTitle();
		post.content = review.getComment() == null ? review.getCourse().getTitle() : review.getComment();
		post.likeCount = 0;
		return post;
	}

	public void increaseLikeCount() {
		this.likeCount++;
	}

	public void decreaseLikeCount() {
		if (this.likeCount > 0) {
			this.likeCount--;
		}
	}

	public void softDelete() {
		this.deletedAt = LocalDateTime.now();
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
