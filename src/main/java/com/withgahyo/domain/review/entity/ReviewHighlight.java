package com.withgahyo.domain.review.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "review_highlight")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewHighlight {

	@EmbeddedId
	private ReviewHighlightId id;

	@MapsId("reviewId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "review_id", nullable = false)
	private Review review;

	@Column(name = "highlight_type", nullable = false, length = 50, insertable = false, updatable = false)
	private String highlightType;

	public static ReviewHighlight create(Review review, String highlightType) {
		ReviewHighlight reviewHighlight = new ReviewHighlight();
		reviewHighlight.id = ReviewHighlightId.of(review.getReviewId(), highlightType);
		reviewHighlight.review = review;
		reviewHighlight.highlightType = highlightType;
		return reviewHighlight;
	}
}
