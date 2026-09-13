package com.withgahyo.domain.review.repository;

import com.withgahyo.domain.review.entity.ReviewHighlight;
import com.withgahyo.domain.review.entity.ReviewHighlightId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewHighlightRepository extends JpaRepository<ReviewHighlight, ReviewHighlightId> {
}
