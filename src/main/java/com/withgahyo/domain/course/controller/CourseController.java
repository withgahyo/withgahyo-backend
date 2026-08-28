package com.withgahyo.domain.course.controller;

import com.withgahyo.domain.course.dto.CourseDetailResponse;
import com.withgahyo.domain.course.dto.CourseFamilyMembersResponse;
import com.withgahyo.domain.course.dto.CourseKeywordSuggestionsResponse;
import com.withgahyo.domain.course.dto.CreateCourseRequest;
import com.withgahyo.domain.course.dto.CreateCourseResponse;
import com.withgahyo.domain.course.service.CourseOptionService;
import com.withgahyo.domain.course.service.CourseService;
import com.withgahyo.global.exception.BusinessException;
import com.withgahyo.global.exception.code.SecurityErrorCode;
import com.withgahyo.global.response.ApiResponse;
import com.withgahyo.global.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CourseController {

	private final CourseService courseService;
	private final CourseOptionService courseOptionService;

	public CourseController(CourseService courseService, CourseOptionService courseOptionService) {
		this.courseService = courseService;
		this.courseOptionService = courseOptionService;
	}

	@GetMapping("/course-keywords/suggestions")
	public ApiResponse<CourseKeywordSuggestionsResponse> getKeywordSuggestions() {
		return ApiResponse.success(courseOptionService.getKeywordSuggestions());
	}

	@GetMapping("/users/me/family-members")
	public ApiResponse<CourseFamilyMembersResponse> getFamilyMembers(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser
	) {
		return ApiResponse.success(courseOptionService.getFamilyMembers(requireUserId(authenticatedUser)));
	}

	@PostMapping("/courses")
	public ApiResponse<CreateCourseResponse> createDraftCourse(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@Valid @RequestBody CreateCourseRequest request
	) {
		return ApiResponse.success(courseService.createDraftCourse(requireUserId(authenticatedUser), request));
	}

	@GetMapping("/courses/{courseId}")
	public ApiResponse<CourseDetailResponse> getCourseDetail(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable Long courseId
	) {
		return ApiResponse.success(courseService.getCourseDetail(requireUserId(authenticatedUser), courseId));
	}

	private Long requireUserId(AuthenticatedUser authenticatedUser) {
		if (authenticatedUser == null) {
			throw new BusinessException(SecurityErrorCode.UNAUTHORIZED);
		}
		return authenticatedUser.userId();
	}
}
