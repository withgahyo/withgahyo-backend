package com.withgahyo.domain.home.service;

import com.withgahyo.domain.course.entity.Course;
import com.withgahyo.domain.course.entity.CourseKeyword;
import com.withgahyo.domain.course.entity.CourseScheduleItem;
import com.withgahyo.domain.course.repository.CourseKeywordRepository;
import com.withgahyo.domain.course.repository.CourseRepository;
import com.withgahyo.domain.course.repository.CourseScheduleItemRepository;
import com.withgahyo.domain.home.dto.HomeResponse;
import com.withgahyo.domain.recommendation.entity.RecommendationCandidate;
import com.withgahyo.domain.recommendation.entity.RecommendationCandidateItem;
import com.withgahyo.domain.recommendation.repository.RecommendationCandidateItemRepository;
import com.withgahyo.domain.recommendation.repository.RecommendationCandidateRepository;
import com.withgahyo.domain.recommendation.service.RecommendationCandidateThumbnails;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class HomeService {

	private final CourseRepository courseRepository;
	private final CourseKeywordRepository courseKeywordRepository;
	private final CourseScheduleItemRepository courseScheduleItemRepository;
	private final RecommendationCandidateRepository recommendationCandidateRepository;
	private final RecommendationCandidateItemRepository recommendationCandidateItemRepository;

	public HomeService(
		CourseRepository courseRepository,
		CourseKeywordRepository courseKeywordRepository,
		CourseScheduleItemRepository courseScheduleItemRepository,
		RecommendationCandidateRepository recommendationCandidateRepository,
		RecommendationCandidateItemRepository recommendationCandidateItemRepository
	) {
		this.courseRepository = courseRepository;
		this.courseKeywordRepository = courseKeywordRepository;
		this.courseScheduleItemRepository = courseScheduleItemRepository;
		this.recommendationCandidateRepository = recommendationCandidateRepository;
		this.recommendationCandidateItemRepository = recommendationCandidateItemRepository;
	}

	@Transactional(readOnly = true)
	public HomeResponse getHome(Long userId) {
		List<Course> courses = courseRepository.findUpcomingCoursesForUser(userId);
		if (courses.isEmpty()) {
			// courseIds가 비어 있는 채로 태그/일정/후보 조회를 호출하면 잘못된 IN () 쿼리가 나갈 수 있으므로
			// 아예 호출하지 않고 빈 응답을 바로 반환한다.
			return HomeResponse.empty();
		}

		List<Long> courseIds = courses.stream().map(Course::getCourseId).toList();
		Map<Long, List<String>> tagsByCourseId = findTagsByCourseId(courseIds);
		Map<Long, String> thumbnailImageUrlByCourseId = findThumbnailImageUrls(courseIds);
		Map<Long, List<RecommendationCandidate>> alternativeCandidatesByCourseId = findAlternativeCandidatesByCourseId(courseIds);
		Map<Long, String> candidateThumbnailImageUrlByCandidateId =
			findCandidateThumbnailImageUrls(alternativeCandidatesByCourseId);

		return HomeResponse.of(
			courses,
			tagsByCourseId,
			thumbnailImageUrlByCourseId,
			alternativeCandidatesByCourseId,
			candidateThumbnailImageUrlByCandidateId
		);
	}

	private Map<Long, List<String>> findTagsByCourseId(List<Long> courseIds) {
		Map<Long, List<String>> tagsByCourseId = new LinkedHashMap<>();
		for (CourseKeyword courseKeyword : courseKeywordRepository.findAllByCourseIdIn(courseIds)) {
			tagsByCourseId
				.computeIfAbsent(courseKeyword.getCourse().getCourseId(), id -> new ArrayList<>())
				.add(courseKeyword.getKeyword().getName());
		}
		return tagsByCourseId;
	}

	// Course 대표 이미지는 확정된 일정을 dayNumber, visitOrder 순으로 훑어 첫 번째 유효한
	// Place.imageUrl을 사용한다(추천 후보 목록의 thumbnailImageUrl 계산과 동일한 정책).
	// Course.imageUrl 컬럼은 실제 생성 흐름에서 세팅되지 않아 참조하지 않는다.
	private Map<Long, String> findThumbnailImageUrls(List<Long> courseIds) {
		Map<Long, String> thumbnailByCourseId = new LinkedHashMap<>();
		for (CourseScheduleItem item : courseScheduleItemRepository.findAllByCourseIdIn(courseIds)) {
			String imageUrl = item.getPlace().getImageUrl();
			if (!StringUtils.hasText(imageUrl)) {
				continue;
			}
			thumbnailByCourseId.putIfAbsent(item.getCourse().getCourseId(), imageUrl);
		}
		return thumbnailByCourseId;
	}

	// Course 하나에 재시도로 생긴 여러 RecommendationJob이 있을 수 있다(course_id는 unique가 아님).
	// job 단위로 후보를 먼저 묶은 뒤, "selected=true 후보가 속한 job"만 그 Course의 확정 job으로
	// 채택하고, 나머지(선택되지 않은) 후보만 alternativeCandidates로 반환한다.
	// rank 순서는 재정렬하지 않고 findAllByJobCourseIdIn의 "... rc.rank asc" 정렬 + 스트림 그룹핑이
	// 원소 순서를 보존하는 성질을 그대로 신뢰한다.
	private Map<Long, List<RecommendationCandidate>> findAlternativeCandidatesByCourseId(List<Long> courseIds) {
		List<RecommendationCandidate> candidates = recommendationCandidateRepository.findAllByJobCourseIdIn(courseIds);
		if (candidates.isEmpty()) {
			return Map.of();
		}

		Map<Long, List<RecommendationCandidate>> candidatesByJobId = candidates.stream()
			.collect(Collectors.groupingBy(
				candidate -> candidate.getRecommendationJob().getRecommendationJobId(),
				LinkedHashMap::new,
				Collectors.toList()
			));

		Map<Long, List<RecommendationCandidate>> alternativesByCourseId = new LinkedHashMap<>();
		for (List<RecommendationCandidate> jobCandidates : candidatesByJobId.values()) {
			boolean isConfirmedJob = jobCandidates.stream().anyMatch(RecommendationCandidate::isSelected);
			if (!isConfirmedJob) {
				// 확정에 쓰이지 않고 버려진 재시도 job -> alternativeCandidates에서 제외
				continue;
			}
			Long courseId = jobCandidates.get(0).getRecommendationJob().getCourse().getCourseId();
			// 정상 흐름에서는 Course.confirm()이 평생 한 번만 확정을 허용하므로, Course당
			// selected=true 후보가 속한 job은 최대 1개다(분석 결과 §5/§12 근거). 방어적으로,
			// 비정상 데이터로 여러 job이 selected를 갖고 있어도 예외를 던지거나 임의로 "최신 job"을
			// 고르지 않고, 먼저 만난(courseId, jobId 오름차순 기준 가장 이른) job만 채택한다.
			alternativesByCourseId.putIfAbsent(
				courseId,
				jobCandidates.stream().filter(candidate -> !candidate.isSelected()).toList()
			);
		}
		return alternativesByCourseId;
	}

	private Map<Long, String> findCandidateThumbnailImageUrls(
		Map<Long, List<RecommendationCandidate>> alternativeCandidatesByCourseId
	) {
		List<Long> candidateIds = alternativeCandidatesByCourseId.values().stream()
			.flatMap(List::stream)
			.map(RecommendationCandidate::getRecommendationCandidateId)
			.toList();
		if (candidateIds.isEmpty()) {
			return Map.of();
		}
		List<RecommendationCandidateItem> items =
			recommendationCandidateItemRepository.findAllWithPlaceByRecommendationCandidateIdIn(candidateIds);
		return RecommendationCandidateThumbnails.firstImageUrlByCandidateId(items);
	}
}
