package com.withgahyo.domain.course.repository;

import com.withgahyo.domain.course.entity.CourseInterestKeyword;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseInterestKeywordRepository extends JpaRepository<CourseInterestKeyword, Long> {

	List<CourseInterestKeyword> findAllByActiveIsTrueOrderByKeywordIdAsc();

	List<CourseInterestKeyword> findAllByKeywordIdInAndActiveIsTrue(List<Long> keywordIds);
}
