package com.withgahyo.domain.course.dto;

import com.withgahyo.domain.course.entity.CourseStatus;
import com.withgahyo.domain.course.entity.TransportMode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CourseDetailResponse(
	Long courseId,
	String title,
	CourseStatus status,
	LocalDate startDate,
	LocalDate endDate,
	long daysUntilTrip,
	RegionResponse region,
	String imageUrl,
	List<String> tags,
	boolean liked,
	long likeCount,
	Long albumId,
	List<ParticipantResponse> participants,
	List<DayResponse> days
) {

	public record RegionResponse(
		String areaCode,
		String sigunguCode,
		String name
	) {
	}

	public record ParticipantResponse(
		Long familyMemberId,
		String name,
		String relationship,
		String profileImageUrl
	) {
	}

	public record DayResponse(
		int day,
		LocalDate date,
		List<PlaceResponse> places
	) {
	}

	public record PlaceResponse(
		Long scheduleItemId,
		int order,
		Long placeId,
		String name,
		String category,
		String address,
		String imageUrl,
		LocalTime arrivalTime,
		LocalTime departureTime,
		BigDecimal latitude,
		BigDecimal longitude,
		List<String> accessibilitySummary,
		TransportToNextResponse transportToNext
	) {
	}

	public record TransportToNextResponse(
		TransportMode mode,
		Integer durationMinutes,
		Integer distanceMeters
	) {
	}
}
