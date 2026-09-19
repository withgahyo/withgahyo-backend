package com.withgahyo.domain.recommendation.service;

import com.withgahyo.domain.recommendation.entity.RecommendationCandidateItem;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * 후보 대표 이미지 계산(여행 순서상 첫 유효 Place.imageUrl)을 순수 계산 로직으로 추출한 유틸리티.
 * {@link RecommendationService}(후보 목록 API)와 {@code HomeService}(Home의 alternativeCandidates)가
 * 동일한 정책을 각자 복제하지 않고 공유하기 위해 둔다. 호출자는
 * {@code RecommendationCandidateItemRepository.findAllWithPlaceByRecommendationCandidateIdIn(...)}
 * 결과(dayNumber asc, visitOrder asc로 정렬됨)를 그대로 넘기면 된다.
 */
public final class RecommendationCandidateThumbnails {

	private RecommendationCandidateThumbnails() {
	}

	public static Map<Long, String> firstImageUrlByCandidateId(List<RecommendationCandidateItem> items) {
		Map<Long, String> thumbnailByCandidateId = new LinkedHashMap<>();
		for (RecommendationCandidateItem item : items) {
			String imageUrl = item.getPlace().getImageUrl();
			if (!StringUtils.hasText(imageUrl)) {
				continue;
			}
			thumbnailByCandidateId.putIfAbsent(
				item.getRecommendationCandidate().getRecommendationCandidateId(),
				imageUrl
			);
		}
		return thumbnailByCandidateId;
	}
}
