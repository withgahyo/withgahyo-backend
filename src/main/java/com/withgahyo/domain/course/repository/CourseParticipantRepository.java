package com.withgahyo.domain.course.repository;

import com.withgahyo.domain.course.entity.CourseParticipant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseParticipantRepository extends JpaRepository<CourseParticipant, Long> {

	@Query("""
		select cp
		from CourseParticipant cp
		join fetch cp.user
		where cp.course.courseId = :courseId
		order by cp.courseParticipantId asc
		""")
	List<CourseParticipant> findAllByCourseId(@Param("courseId") Long courseId);

	@Query("""
		select count(cp) > 0
		from CourseParticipant cp
		where cp.course.courseId = :courseId
			and cp.user.userId = :userId
		""")
	boolean existsByCourseIdAndUserId(@Param("courseId") Long courseId, @Param("userId") Long userId);
}
