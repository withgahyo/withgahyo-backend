package com.withgahyo.domain.course.repository;

import com.withgahyo.domain.course.entity.CourseLike;
import com.withgahyo.domain.course.entity.CourseLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseLikeRepository extends JpaRepository<CourseLike, CourseLikeId> {

	long countByCourseCourseId(Long courseId);
}
