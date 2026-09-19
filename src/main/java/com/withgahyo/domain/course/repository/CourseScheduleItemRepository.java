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

	// Home 대표 이미지 계산용: 여러 Course의 일정+장소를 한 번에 조회한다(N+1 방지).
	// dayNumber, visitOrder로만 정렬해도 각 Course 내부의 방문 순서는 그대로 보존되므로,
	// Service에서 courseId별 "첫 유효 imageUrl"만 뽑아내는 데 문제가 없다.
	@Query("""
		select csi
		from CourseScheduleItem csi
		join fetch csi.place
		where csi.course.courseId in :courseIds
		order by csi.dayNumber asc, csi.visitOrder asc
		""")
	List<CourseScheduleItem> findAllByCourseIdIn(@Param("courseIds") List<Long> courseIds);
}
