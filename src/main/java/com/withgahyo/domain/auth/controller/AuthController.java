package com.withgahyo.domain.auth.controller;

import com.withgahyo.domain.auth.dto.AuthTokenResponse;
import com.withgahyo.domain.auth.dto.SocialLoginRequest;
import com.withgahyo.domain.auth.dto.TokenRefreshRequest;
import com.withgahyo.domain.auth.dto.TokenRefreshResponse;
import com.withgahyo.domain.auth.service.AuthService;
import com.withgahyo.global.exception.BusinessException;
import com.withgahyo.global.exception.code.SecurityErrorCode;
import com.withgahyo.global.response.ApiResponse;
import com.withgahyo.global.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
		return ApiResponse.success(authService.loginWithKakao(request.authorizationCode(), request.redirectUri()));
	}

	@PostMapping("/login/google")
	public ApiResponse<AuthTokenResponse> loginWithGoogle(@Valid @RequestBody SocialLoginRequest request) {
		return ApiResponse.success(authService.loginWithGoogle(request.authorizationCode(), request.redirectUri()));
	}

	@PostMapping("/logout")
	public ApiResponse<Void> logout(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		if (authenticatedUser == null) {
			throw new BusinessException(SecurityErrorCode.UNAUTHORIZED);
		}
		authService.logout(authenticatedUser.userId());
		return ApiResponse.ok();
	}

	@PostMapping("/token/refresh")
	public ApiResponse<TokenRefreshResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
		return ApiResponse.success(authService.refresh(request.refreshToken()));
	}
}
