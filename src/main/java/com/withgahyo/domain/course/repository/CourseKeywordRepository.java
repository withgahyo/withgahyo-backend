package com.withgahyo.domain.course.repository;

import com.withgahyo.domain.course.entity.CourseKeyword;
import com.withgahyo.domain.course.entity.CourseKeywordId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseKeywordRepository extends JpaRepository<CourseKeyword, CourseKeywordId> {
}
