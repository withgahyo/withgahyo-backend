package com.withgahyo.domain.course.repository;

import com.withgahyo.domain.course.entity.Course;
import java.util.List;
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

	@Query("""
		select c
		from Course c
		join fetch c.region
		where c.creatorUser.userId = :userId
			and c.status = com.withgahyo.domain.course.entity.CourseStatus.COMPLETED
			and c.deletedAt is null
			and not exists (
				select 1
				from Review r
				where r.course = c
					and r.user.userId = :userId
			)
		order by c.endDate desc, c.courseId desc
	""")
	java.util.List<Course> findReviewPendingCourses(@Param("userId") Long userId);

	// Home 캐러셀용: 본인이 생성했거나 가족 구성원으로 참여 중인 확정(UPCOMING) 코스만 조회한다.
	// EXISTS 서브쿼리를 쓰므로 creator/participant 조건이 겹쳐도 같은 Course가 두 번 반환되지 않는다.
	@Query("""
		select c
		from Course c
		join fetch c.region
		where c.deletedAt is null
			and c.status = com.withgahyo.domain.course.entity.CourseStatus.UPCOMING
			and (
				c.creatorUser.userId = :userId
				or exists (
					select 1
					from CourseParticipant cp
					where cp.course = c
						and cp.user.userId = :userId
				)
			)
		order by c.startDate asc
	""")
	List<Course> findUpcomingCoursesForUser(@Param("userId") Long userId);
}
