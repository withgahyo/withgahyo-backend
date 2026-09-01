package com.withgahyo.domain.course.repository;

import com.withgahyo.domain.course.entity.CourseScheduleItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseScheduleItemRepository extends JpaRepository<CourseScheduleItem, Long> {

	@Query("""
		select csi
		from CourseScheduleItem csi
		join fetch csi.place p
		where csi.course.courseId = :courseId
		order by csi.dayNumber asc, csi.visitOrder asc
		""")
	List<CourseScheduleItem> findAllByCourseId(@Param("courseId") Long courseId);

	List<CourseScheduleItem> findAllByCourseCourseIdOrderByDayNumberAscVisitOrderAsc(Long courseId);
}
