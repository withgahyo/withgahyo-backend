package com.withgahyo.domain.home.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.withgahyo.domain.home.dto.HomeResponse;
import com.withgahyo.domain.home.service.HomeService;
import com.withgahyo.global.exception.BusinessException;
import com.withgahyo.global.exception.code.SecurityErrorCode;
import com.withgahyo.global.response.ApiResponse;
import com.withgahyo.global.security.AuthenticatedUser;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

	@Mock
	private HomeService homeService;

	private HomeController homeController;

	@BeforeEach
	void setUp() {
		homeController = new HomeController(homeService);
	}

	@Test
	void getHome_returnsHomeResponse() {
		HomeResponse serviceResponse = new HomeResponse(List.of(
			new HomeResponse.FamilyCourseResponse(
				12L, "대전 가족 여행", "대전", "https://example.com/place.jpg",
				java.time.LocalDate.now().plusDays(6), 6L, List.of("무장애", "맛집"),
				List.of(new HomeResponse.AlternativeCandidateResponse(
					31L, "여유로운 대전 산책", "요약", "https://example.com/alt.jpg"
				))
			)
		));
		given(homeService.getHome(1L)).willReturn(serviceResponse);

		ApiResponse<HomeResponse> response = homeController.getHome(new AuthenticatedUser(1L));

		assertThat(response.data()).isEqualTo(serviceResponse);
		verify(homeService).getHome(1L);
	}

	@Test
	void getHome_returnsEmptyFamilyCourses_whenServiceReturnsEmpty() {
		given(homeService.getHome(1L)).willReturn(HomeResponse.empty());

		ApiResponse<HomeResponse> response = homeController.getHome(new AuthenticatedUser(1L));

		assertThat(response.data().familyCourses()).isEmpty();
	}

	@Test
	void getHome_fail_whenNotAuthenticated() {
		assertThatThrownBy(() -> homeController.getHome(null))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(SecurityErrorCode.UNAUTHORIZED);
	}
}
