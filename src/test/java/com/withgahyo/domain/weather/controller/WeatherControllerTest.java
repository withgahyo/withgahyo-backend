package com.withgahyo.domain.weather.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.withgahyo.domain.weather.dto.UpcomingWeatherResponse;
import com.withgahyo.domain.weather.dto.WeatherStatus;
import com.withgahyo.domain.weather.service.WeatherService;
import com.withgahyo.global.exception.BusinessException;
import com.withgahyo.global.exception.code.SecurityErrorCode;
import com.withgahyo.global.response.ApiResponse;
import com.withgahyo.global.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WeatherControllerTest {

	@Mock
	private WeatherService weatherService;

	private WeatherController weatherController;

	@BeforeEach
	void setUp() {
		weatherController = new WeatherController(weatherService);
	}

	@Test
	void getUpcomingWeather_returnsServiceResponse() {
		UpcomingWeatherResponse serviceResponse = UpcomingWeatherResponse.noUpcomingTrip();
		given(weatherService.getUpcomingWeather(1L)).willReturn(serviceResponse);

		ApiResponse<UpcomingWeatherResponse> response = weatherController.getUpcomingWeather(new AuthenticatedUser(1L));

		assertThat(response.data()).isEqualTo(serviceResponse);
		assertThat(response.data().status()).isEqualTo(WeatherStatus.NO_UPCOMING_TRIP);
		verify(weatherService).getUpcomingWeather(1L);
	}

	@Test
	void getUpcomingWeather_fail_whenNotAuthenticated() {
		assertThatThrownBy(() -> weatherController.getUpcomingWeather(null))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(SecurityErrorCode.UNAUTHORIZED);
	}
}
