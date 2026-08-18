package com.withgahyo.domain.auth.controller;

import com.withgahyo.domain.auth.dto.AuthTokenResponse;
import com.withgahyo.domain.auth.dto.SocialLoginRequest;
import com.withgahyo.domain.auth.service.AuthService;
import com.withgahyo.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login/kakao")
	public ApiResponse<AuthTokenResponse> loginWithKakao(@Valid @RequestBody SocialLoginRequest request) {
		return ApiResponse.success(authService.loginWithKakao(request.oauthAccessToken()));
	}

	@PostMapping("/login/google")
	public ApiResponse<AuthTokenResponse> loginWithGoogle(@Valid @RequestBody SocialLoginRequest request) {
		return ApiResponse.success(authService.loginWithGoogle(request.oauthAccessToken()));
	}
}
