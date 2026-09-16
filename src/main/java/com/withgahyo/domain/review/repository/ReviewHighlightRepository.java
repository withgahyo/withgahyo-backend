package com.withgahyo.domain.review.repository;

import com.withgahyo.domain.review.entity.ReviewHighlight;
import com.withgahyo.domain.review.entity.ReviewHighlightId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewHighlightRepository extends JpaRepository<ReviewHighlight, ReviewHighlightId> {

	List<ReviewHighlight> findByReview_ReviewId(Long reviewId);

	List<ReviewHighlight> findByReview_ReviewIdIn(List<Long> reviewIds);
}
