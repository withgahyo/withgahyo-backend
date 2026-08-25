package com.withgahyo.domain.course.entity;

import com.withgahyo.domain.place.entity.Place;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "course_must_visit_place")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseMustVisitPlace {

	@EmbeddedId
	private CourseMustVisitPlaceId id;

	@MapsId("courseId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "course_id", nullable = false)
	private Course course;

	@MapsId("placeId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "place_id", nullable = false)
	private Place place;

	public static CourseMustVisitPlace create(Course course, Place place) {
		CourseMustVisitPlace mustVisitPlace = new CourseMustVisitPlace();
		mustVisitPlace.id = new CourseMustVisitPlaceId(course.getCourseId(), place.getPlaceId());
		mustVisitPlace.course = course;
		mustVisitPlace.place = place;
		return mustVisitPlace;
	}
}
