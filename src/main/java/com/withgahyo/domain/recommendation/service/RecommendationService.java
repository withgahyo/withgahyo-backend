package com.withgahyo.domain.recommendation.service;

import com.withgahyo.domain.album.entity.Album;
import com.withgahyo.domain.album.repository.AlbumRepository;
import com.withgahyo.domain.course.entity.Course;
import com.withgahyo.domain.course.entity.CourseScheduleItem;
import com.withgahyo.domain.course.repository.CourseParticipantRepository;
import com.withgahyo.domain.course.repository.CourseRepository;
import com.withgahyo.domain.course.repository.CourseScheduleItemRepository;
import com.withgahyo.domain.recommendation.dto.RecommendationCandidateDetailResponse;
import com.withgahyo.domain.recommendation.dto.RecommendationCandidatesResponse;
import com.withgahyo.domain.recommendation.dto.RecommendationStatusResponse;
import com.withgahyo.domain.recommendation.dto.SelectRecommendationCandidateRequest;
import com.withgahyo.domain.recommendation.dto.SelectRecommendationCandidateResponse;
import com.withgahyo.domain.recommendation.dto.StartRecommendationRequest;
import com.withgahyo.domain.recommendation.dto.StartRecommendationResponse;
import com.withgahyo.domain.recommendation.entity.RecommendationCandidate;
import com.withgahyo.domain.recommendation.entity.RecommendationCandidateItem;
import com.withgahyo.domain.recommendation.entity.RecommendationJob;
import com.withgahyo.domain.recommendation.entity.RecommendationJobStatus;
import com.withgahyo.domain.recommendation.exception.RecommendationErrorCode;
import com.withgahyo.domain.recommendation.repository.RecommendationCandidateItemRepository;
import com.withgahyo.domain.recommendation.repository.RecommendationCandidateRepository;
import com.withgahyo.domain.recommendation.repository.RecommendationJobRepository;
import com.withgahyo.global.exception.BusinessException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationService {

	private final RecommendationJobRepository recommendationJobRepository;
	private final RecommendationCandidateRepository recommendationCandidateRepository;
	private final RecommendationCandidateItemRepository recommendationCandidateItemRepository;
	private final CourseRepository courseRepository;
	private final CourseParticipantRepository courseParticipantRepository;
	private final CourseScheduleItemRepository courseScheduleItemRepository;
	private final AlbumRepository albumRepository;

	public RecommendationService(
		RecommendationJobRepository recommendationJobRepository,
		RecommendationCandidateRepository recommendationCandidateRepository,
		RecommendationCandidateItemRepository recommendationCandidateItemRepository,
		CourseRepository courseRepository,
		CourseParticipantRepository courseParticipantRepository,
		CourseScheduleItemRepository courseScheduleItemRepository,
		AlbumRepository albumRepository
	) {
		this.recommendationJobRepository = recommendationJobRepository;
		this.recommendationCandidateRepository = recommendationCandidateRepository;
		this.recommendationCandidateItemRepository = recommendationCandidateItemRepository;
		this.courseRepository = courseRepository;
		this.courseParticipantRepository = courseParticipantRepository;
		this.courseScheduleItemRepository = courseScheduleItemRepository;
		this.albumRepository = albumRepository;
	}

	@Transactional
	public StartRecommendationResponse startGeneration(
		Long userId,
		Long courseId,
		StartRecommendationRequest request
	) {
		Course course = findCourse(courseId);
		validateCourseAccess(userId, course);

		RecommendationJob job = recommendationJobRepository.save(RecommendationJob.createPending(course));
		return StartRecommendationResponse.from(job);
	}

	@Transactional(readOnly = true)
	public RecommendationStatusResponse getGenerationStatus(Long userId, Long generationId) {
		RecommendationJob job = findJob(generationId);
		validateCourseAccess(userId, job.getCourse());
		return RecommendationStatusResponse.from(job);
	}

	@Transactional(readOnly = true)
	public RecommendationCandidatesResponse getCandidates(Long userId, Long generationId) {
		RecommendationJob job = findCompletedJobForUser(userId, generationId);
		List<RecommendationCandidate> candidates =
			recommendationCandidateRepository.findAllByRecommendationJobRecommendationJobIdOrderByRankAsc(generationId);
		return RecommendationCandidatesResponse.of(job, candidates);
	}

	@Transactional(readOnly = true)
	public RecommendationCandidateDetailResponse getCandidateDetail(Long userId, Long generationId, Long candidateId) {
		findCompletedJobForUser(userId, generationId);
		RecommendationCandidate candidate = findCandidate(generationId, candidateId);
		List<RecommendationCandidateItem> items = findCandidateItems(candidateId);
		return RecommendationCandidateDetailResponse.of(candidate, items);
	}

	@Transactional
	public SelectRecommendationCandidateResponse selectCandidate(
		Long userId,
		Long generationId,
		SelectRecommendationCandidateRequest request
	) {
		RecommendationJob job = findCompletedJobForUser(userId, generationId);
		RecommendationCandidate candidate = findCandidate(generationId, request.candidateId());
		RecommendationCandidate selectedCandidate = recommendationCandidateRepository
			.findSelectedByRecommendationJobId(generationId)
			.orElse(null);

		if (selectedCandidate != null) {
			if (!selectedCandidate.getRecommendationCandidateId().equals(candidate.getRecommendationCandidateId())) {
				throw new BusinessException(RecommendationErrorCode.RECOMMENDATION_CANDIDATE_ALREADY_SELECTED);
			}
			Album album = findOrCreateAlbum(job.getCourse());
			return toSelectionResponse(job, selectedCandidate, album);
		}

		List<RecommendationCandidateItem> items = findCandidateItems(candidate.getRecommendationCandidateId());
		List<CourseScheduleItem> scheduleItems = items.stream()
			.map(item -> CourseScheduleItem.from(job.getCourse(), item))
			.toList();

		courseScheduleItemRepository.saveAll(scheduleItems);
		candidate.select();
		job.getCourse().confirm();
		Album album = findOrCreateAlbum(job.getCourse());

		return toSelectionResponse(job, candidate, album);
	}

	private RecommendationJob findCompletedJobForUser(Long userId, Long generationId) {
		RecommendationJob job = findJob(generationId);
		validateCourseAccess(userId, job.getCourse());
		if (job.getStatus() != RecommendationJobStatus.COMPLETED) {
			throw new BusinessException(RecommendationErrorCode.RECOMMENDATION_JOB_NOT_COMPLETED);
		}
		return job;
	}

	private RecommendationJob findJob(Long generationId) {
		return recommendationJobRepository.findWithCourseByRecommendationJobId(generationId)
			.orElseThrow(() -> new BusinessException(RecommendationErrorCode.RECOMMENDATION_JOB_NOT_FOUND));
	}

	private RecommendationCandidate findCandidate(Long generationId, Long candidateId) {
		return recommendationCandidateRepository
			.findByRecommendationCandidateIdAndRecommendationJobRecommendationJobId(candidateId, generationId)
			.orElseThrow(() -> new BusinessException(RecommendationErrorCode.RECOMMENDATION_CANDIDATE_NOT_FOUND));
	}

	private List<RecommendationCandidateItem> findCandidateItems(Long candidateId) {
		return recommendationCandidateItemRepository
			.findAllByRecommendationCandidateRecommendationCandidateIdOrderByDayNumberAscVisitOrderAsc(candidateId);
	}

	private Course findCourse(Long courseId) {
		Course course = courseRepository.findById(courseId)
			.orElseThrow(() -> new BusinessException(RecommendationErrorCode.COURSE_NOT_FOUND));
		if (course.isDeleted()) {
			throw new BusinessException(RecommendationErrorCode.COURSE_NOT_FOUND);
		}
		return course;
	}

	private void validateCourseAccess(Long userId, Course course) {
		if (course.getCreatorUser().getUserId().equals(userId)) {
			return;
		}
		if (courseParticipantRepository.existsByCourseIdAndUserId(course.getCourseId(), userId)) {
			return;
		}
		throw new BusinessException(RecommendationErrorCode.RECOMMENDATION_ACCESS_DENIED);
	}

	private Album findOrCreateAlbum(Course course) {
		return albumRepository.findFirstByCourseCourseIdOrderByAlbumIdAsc(course.getCourseId())
			.orElseGet(() -> albumRepository.save(Album.create(course)));
	}

	private SelectRecommendationCandidateResponse toSelectionResponse(
		RecommendationJob job,
		RecommendationCandidate candidate,
		Album album
	) {
		return new SelectRecommendationCandidateResponse(
			job.getCourse().getCourseId(),
			job.getRecommendationJobId(),
			candidate.getRecommendationCandidateId(),
			job.getCourse().getStatus(),
			album.getAlbumId(),
			job.getCourse().getConfirmedAt()
		);
	}
}
