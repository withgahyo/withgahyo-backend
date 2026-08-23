package com.withgahyo.domain.course.repository;

import com.withgahyo.domain.course.entity.CourseMustVisitPlace;
import com.withgahyo.domain.course.entity.CourseMustVisitPlaceId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseMustVisitPlaceRepository extends JpaRepository<CourseMustVisitPlace, CourseMustVisitPlaceId> {
}
