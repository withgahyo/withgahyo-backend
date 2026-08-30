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
}
