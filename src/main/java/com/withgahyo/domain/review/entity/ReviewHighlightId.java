package com.withgahyo.domain.review.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
public class ReviewHighlightId implements Serializable {

	@Column(name = "review_id")
	private Long reviewId;

	@Column(name = "highlight_type")
	private String highlightType;
}
