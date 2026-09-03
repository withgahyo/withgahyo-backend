package com.withgahyo.domain.course.repository;

import com.withgahyo.domain.course.entity.CourseParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseParticipantRepository extends JpaRepository<CourseParticipant, Long> {
}
