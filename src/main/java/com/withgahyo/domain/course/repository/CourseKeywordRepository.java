package com.withgahyo.domain.course.repository;

import com.withgahyo.domain.course.entity.CourseKeyword;
import com.withgahyo.domain.course.entity.CourseKeywordId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseKeywordRepository extends JpaRepository<CourseKeyword, CourseKeywordId> {

	@Query("""
		select ck
		from CourseKeyword ck
		join fetch ck.keyword
		where ck.course.courseId = :courseId
		order by ck.keyword.keywordId asc
		""")
	List<CourseKeyword> findAllByCourseId(@Param("courseId") Long courseId);

	// Home처럼 여러 Course의 태그를 한 번에 보여줘야 할 때, Course 개수만큼 반복 조회(N+1)하지 않도록
	// courseIds를 한 번에 받아 조회한다. Service에서 courseId 기준으로 grouping한다.
	@Query("""
		select ck
		from CourseKeyword ck
		join fetch ck.keyword
		where ck.course.courseId in :courseIds
		order by ck.course.courseId asc, ck.keyword.keywordId asc
		""")
	List<CourseKeyword> findAllByCourseIdIn(@Param("courseIds") List<Long> courseIds);
}
