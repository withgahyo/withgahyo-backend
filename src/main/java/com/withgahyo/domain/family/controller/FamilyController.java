package com.withgahyo.domain.family.controller;

import com.withgahyo.domain.family.dto.ConnectFamilyMemberRequest;
import com.withgahyo.domain.family.dto.FamilyMemberCandidateResponse;
import com.withgahyo.domain.family.dto.FamilyMemberResponse;
import com.withgahyo.domain.family.service.FamilyService;
import com.withgahyo.global.exception.BusinessException;
import com.withgahyo.global.exception.code.SecurityErrorCode;
import com.withgahyo.global.response.ApiResponse;
import com.withgahyo.global.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/family/members")
public class FamilyController {

	private final FamilyService familyService;

	public FamilyController(FamilyService familyService) {
		this.familyService = familyService;
	}

	@GetMapping("/candidates")
	public ApiResponse<FamilyMemberCandidateResponse> findCandidate(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@RequestParam @NotBlank @Email String email
	) {
		return ApiResponse.success(familyService.findCandidate(requireUserId(authenticatedUser), email));
	}

	@PostMapping
	public ApiResponse<FamilyMemberResponse> connect(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@Valid @RequestBody ConnectFamilyMemberRequest request
	) {
		return ApiResponse.success(familyService.connect(requireUserId(authenticatedUser), request));
	}

	private Long requireUserId(AuthenticatedUser authenticatedUser) {
		if (authenticatedUser == null) {
			throw new BusinessException(SecurityErrorCode.UNAUTHORIZED);
		}
		return authenticatedUser.userId();
	}
}
