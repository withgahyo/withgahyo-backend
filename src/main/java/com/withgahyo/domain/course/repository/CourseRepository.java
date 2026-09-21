package com.withgahyo.domain.course.repository;

import com.withgahyo.domain.course.entity.Course;
import java.time.LocalDate;
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

	// 날씨 기능용: findUpcomingCoursesForUser는 Home 캐러셀에서 지난 UPCOMING 코스까지 의도적으로
	// 보여주기 위한 쿼리라 재사용하지 않는다. 이 쿼리는 startDate >= :today 조건을 추가로 걸어
	// "아직 시작하지 않은" 가장 가까운 확정 코스만 남기고, courseId를 2차 정렬키로 둬서
	// 같은 startDate가 여럿이어도 항상 같은 순서를 보장한다. 결과 1건만 필요하면 호출부에서 첫 번째만 쓴다.
	@Query("""
		select c
		from Course c
		join fetch c.region
		where c.deletedAt is null
			and c.status = com.withgahyo.domain.course.entity.CourseStatus.UPCOMING
			and c.startDate >= :today
			and (
				c.creatorUser.userId = :userId
				or exists (
					select 1
					from CourseParticipant cp
					where cp.course = c
						and cp.user.userId = :userId
				)
			)
		order by c.startDate asc, c.courseId asc
	""")
	List<Course> findUpcomingCoursesFromDateForUser(@Param("userId") Long userId, @Param("today") LocalDate today);
}
