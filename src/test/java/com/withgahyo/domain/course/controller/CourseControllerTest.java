package com.withgahyo.domain.course.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.withgahyo.domain.course.dto.CourseConfirmResponse;
import com.withgahyo.domain.course.dto.CourseLikeResponse;
import com.withgahyo.domain.course.dto.UpdateCourseRequest;
import com.withgahyo.domain.course.dto.UpdateCourseResponse;
import com.withgahyo.domain.course.service.CourseOptionService;
import com.withgahyo.domain.course.service.CourseService;
import com.withgahyo.global.security.AuthenticatedUser;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseControllerTest {

	@Mock
	private CourseService courseService;

	@Mock
	private CourseOptionService courseOptionService;

	private CourseController courseController;

	@BeforeEach
	void setUp() {
		courseController = new CourseController(courseService, courseOptionService);
	}

	@Test
	void likeCourse_returnsLikedResponse() {
		CourseLikeResponse serviceResponse = new CourseLikeResponse(301L, true);

		given(courseService.likeCourse(1L, 301L)).willReturn(serviceResponse);

		var response = courseController.likeCourse(new AuthenticatedUser(1L), 301L);

		assertThat(response.data()).isEqualTo(serviceResponse);
		verify(courseService).likeCourse(1L, 301L);
	}

	@Test
	void unlikeCourse_returnsUnlikedResponse() {
		CourseLikeResponse serviceResponse = new CourseLikeResponse(301L, false);

		given(courseService.unlikeCourse(1L, 301L)).willReturn(serviceResponse);

		var response = courseController.unlikeCourse(new AuthenticatedUser(1L), 301L);

		assertThat(response.data()).isEqualTo(serviceResponse);
		verify(courseService).unlikeCourse(1L, 301L);
	}

	@Test
	void confirmCourse_returnsConfirmResponse() {
		CourseConfirmResponse serviceResponse = new CourseConfirmResponse(301L, true);

		given(courseService.confirmCourse(1L, 301L)).willReturn(serviceResponse);

		var response = courseController.confirmCourse(new AuthenticatedUser(1L), 301L);

		assertThat(response.data()).isEqualTo(serviceResponse);
		verify(courseService).confirmCourse(1L, 301L);
	}

	@Test
	void updateCourse_returnsUpdateResponse() {
		UpdateCourseRequest request = new UpdateCourseRequest("대전 힐링 여행");
		UpdateCourseResponse serviceResponse = new UpdateCourseResponse(301L, "대전 힐링 여행", LocalDateTime.now());

		given(courseService.updateCourseBasicInfo(1L, 301L, request)).willReturn(serviceResponse);

		var response = courseController.updateCourse(new AuthenticatedUser(1L), 301L, request);

		assertThat(response.data()).isEqualTo(serviceResponse);
		verify(courseService).updateCourseBasicInfo(1L, 301L, request);
	}

	@Test
	void deleteCourse_returnsEmptySuccess() {
		var response = courseController.deleteCourse(new AuthenticatedUser(1L), 301L);

		assertThat(response.data()).isNull();
		verify(courseService).deleteCourse(1L, 301L);
	}
}
