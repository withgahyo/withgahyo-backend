package com.withgahyo.domain.course.repository;

import com.withgahyo.domain.course.entity.Course;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long> {

	@Query("""
		select c
		from Course c
		join fetch c.creatorUser
		join fetch c.region
		where c.courseId = :courseId
			and c.deletedAt is null
		""")
	Optional<Course> findActiveById(@Param("courseId") Long courseId);
}
