package com.withgahyo.domain.course.repository;

import com.withgahyo.domain.course.entity.CourseMustVisitPlace;
import com.withgahyo.domain.course.entity.CourseMustVisitPlaceId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseMustVisitPlaceRepository extends JpaRepository<CourseMustVisitPlace, CourseMustVisitPlaceId> {

	@Query("""
		select cmvp
		from CourseMustVisitPlace cmvp
		join fetch cmvp.place
		where cmvp.course.courseId = :courseId
		order by cmvp.place.placeId asc
		""")
	List<CourseMustVisitPlace> findAllByCourseId(@Param("courseId") Long courseId);
}
