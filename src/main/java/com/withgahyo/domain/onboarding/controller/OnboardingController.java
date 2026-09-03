package com.withgahyo.domain.onboarding.controller;

import com.withgahyo.domain.onboarding.dto.ConditionsRequest;
import com.withgahyo.domain.onboarding.dto.FoodPreferenceOptionResponse;
import com.withgahyo.domain.onboarding.dto.FoodPreferenceSaveRequest;
import com.withgahyo.domain.onboarding.dto.OnboardingResponse;
import com.withgahyo.domain.onboarding.dto.OnboardingSaveRequest;
import com.withgahyo.domain.onboarding.dto.TourismPreferenceOptionResponse;
import com.withgahyo.domain.onboarding.dto.TourismPreferenceSaveRequest;
import com.withgahyo.domain.onboarding.service.OnboardingService;
import com.withgahyo.global.exception.BusinessException;
import com.withgahyo.global.exception.code.SecurityErrorCode;
import com.withgahyo.global.response.ApiResponse;
import com.withgahyo.global.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Onboarding", description = "사용자 온보딩 및 여행 취향 설정 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OnboardingController {

	private final OnboardingService onboardingService;

	@Operation(
		summary = "온보딩 정보 조회",
		description = "로그인한 사용자의 온보딩 진행 상태를 조회합니다. 온보딩 프로필이 아직 없어도 새로 생성하지 않고, "
			+ "필드가 비어 있고 onboardingCompleted가 false인 기본값 응답을 반환합니다."
	)
	@GetMapping("/users/me/onboarding")
	public ApiResponse<OnboardingResponse> getOnboarding(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		return ApiResponse.success(onboardingService.getOnboarding(requireUserId(authenticatedUser)));
	}

	@Operation(
		summary = "온보딩 정보 저장·수정",
		description = "여행 컨디션(보행/휴식/계단/경사/매운맛 선호), 관광 취향, 식사 취향을 한 번에 저장하거나 수정합니다. "
			+ "컨디션 항목은 전부 선택 입력이라 값이 없으면 그대로 비워둡니다. "
			+ "관광/식사 취향은 하나의 트랜잭션 안에서 기존 매핑을 전부 삭제한 뒤 요청받은 값으로 재삽입합니다."
	)
	@PutMapping("/users/me/onboarding")
	public ApiResponse<Void> saveOnboarding(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@Valid @RequestBody OnboardingSaveRequest request
	) {
		onboardingService.saveOnboarding(requireUserId(authenticatedUser), request);
		return ApiResponse.ok();
	}

	@Operation(
		summary = "관광 취향 선택지 조회",
		description = "온보딩에서 선택할 수 있는 관광 취향 선택지 목록을 조회합니다. 비활성화(is_active=false) 처리된 항목은 응답에서 제외됩니다."
	)
	@GetMapping("/onboarding/tourism-preferences")
	public ApiResponse<List<TourismPreferenceOptionResponse>> getTourismPreferenceOptions() {
		return ApiResponse.success(onboardingService.getTourismPreferenceOptions());
	}

	@Operation(
		summary = "식사 취향 선택지 조회",
		description = "온보딩에서 선택할 수 있는 식사 취향 선택지 목록을 조회합니다. 비활성화(is_active=false) 처리된 항목은 응답에서 제외됩니다."
	)
	@GetMapping("/onboarding/food-preferences")
	public ApiResponse<List<FoodPreferenceOptionResponse>> getFoodPreferenceOptions() {
		return ApiResponse.success(onboardingService.getFoodPreferenceOptions());
	}

	@Operation(
		summary = "관광 취향 저장",
		description = "사용자가 선택한 관광 취향 목록을 저장합니다. 기존 매핑을 전부 삭제한 뒤 요청받은 ID 목록으로 재삽입하며, "
			+ "존재하지 않는 ID가 포함되면 실패합니다."
	)
	@PatchMapping("/users/me/onboarding/tourism-preferences")
	public ApiResponse<Void> saveTourismPreferences(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@Valid @RequestBody TourismPreferenceSaveRequest request
	) {
		onboardingService.saveTourismPreferences(requireUserId(authenticatedUser), request);
		return ApiResponse.ok();
	}

	@Operation(
		summary = "식사 취향 저장",
		description = "사용자가 선택한 식사 취향 목록을 저장합니다. 기존 매핑을 전부 삭제한 뒤 요청받은 ID 목록으로 재삽입하며, "
			+ "존재하지 않는 ID가 포함되면 실패합니다."
	)
	@PatchMapping("/users/me/onboarding/food-preferences")
	public ApiResponse<Void> saveFoodPreferences(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@Valid @RequestBody FoodPreferenceSaveRequest request
	) {
		onboardingService.saveFoodPreferences(requireUserId(authenticatedUser), request);
		return ApiResponse.ok();
	}

	@Operation(
		summary = "여행 컨디션 저장",
		description = "보행/휴식/계단/경사/매운맛 선호 등 여행 컨디션 항목을 저장합니다. 전부 선택 입력이라 일부 또는 전부 비워서 저장할 수 있고, "
			+ "온보딩 프로필이 아직 없으면 새로 생성한 뒤 값을 반영합니다."
	)
	@PatchMapping("/users/me/onboarding/conditions")
	public ApiResponse<Void> saveConditions(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@Valid @RequestBody ConditionsRequest request
	) {
		onboardingService.saveConditions(requireUserId(authenticatedUser), request);
		return ApiResponse.ok();
	}

	@Operation(
		summary = "온보딩 완료",
		description = "관광 취향 1개 이상, 식사 취향 1개 이상이 등록되었는지 검증한 뒤 온보딩을 완료 처리합니다. "
			+ "여행 컨디션 항목은 개인화 추천을 위한 선택 정보라 완료 조건에 포함되지 않습니다. "
			+ "필수 항목이 누락되면 도메인 검증 오류를 반환하고, 이미 완료된 상태에서 다시 호출하면 상태 변경 없이 성공으로 처리합니다(idempotent)."
	)
	@PostMapping("/users/me/onboarding/complete")
	public ApiResponse<Void> completeOnboarding(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		onboardingService.completeOnboarding(requireUserId(authenticatedUser));
		return ApiResponse.ok();
	}

	private Long requireUserId(AuthenticatedUser authenticatedUser) {
		if (authenticatedUser == null) {
			throw new BusinessException(SecurityErrorCode.UNAUTHORIZED);
		}
		return authenticatedUser.userId();
	}
}
