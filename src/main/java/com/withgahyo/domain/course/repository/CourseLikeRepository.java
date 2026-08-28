package com.withgahyo.domain.course.repository;

import com.withgahyo.domain.course.entity.CourseLike;
import com.withgahyo.domain.course.entity.CourseLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseLikeRepository extends JpaRepository<CourseLike, CourseLikeId> {

	long countByCourseCourseId(Long courseId);

	@Modifying
	@Query(
		value = """
			insert ignore into course_like (user_id, course_id, created_at)
			values (:userId, :courseId, current_timestamp)
			""",
		nativeQuery = true
	)
	int insertIgnore(@Param("userId") Long userId, @Param("courseId") Long courseId);

	@Modifying
	@Query("""
		delete from CourseLike cl
		where cl.id.userId = :userId
			and cl.id.courseId = :courseId
		""")
	int deleteByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);
}
