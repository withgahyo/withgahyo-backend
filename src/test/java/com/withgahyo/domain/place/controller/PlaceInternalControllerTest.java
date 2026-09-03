package com.withgahyo.domain.place.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.withgahyo.domain.place.dto.InternalPlaceBatchGetRequest;
import com.withgahyo.domain.place.dto.InternalPlaceBatchGetResponse;
import com.withgahyo.domain.place.dto.InternalPlaceUpsertRequest;
import com.withgahyo.domain.place.dto.InternalPlaceUpsertResponse;
import com.withgahyo.domain.place.service.PlaceInternalService;
import com.withgahyo.global.exception.BusinessException;
import com.withgahyo.global.exception.code.SecurityErrorCode;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlaceInternalControllerTest {

	@Mock
	private PlaceInternalService placeInternalService;

	private PlaceInternalController placeInternalController;

	@BeforeEach
	void setUp() {
		placeInternalController = new PlaceInternalController(placeInternalService, "test-internal-key");
	}

	@Test
	void upsertPlaces_success_returnsUpsertResponse() {
		InternalPlaceUpsertRequest request = request();
		InternalPlaceUpsertResponse serviceResponse = new InternalPlaceUpsertResponse(List.of(
			new InternalPlaceUpsertResponse.PlaceResponse(501L, "TOUR_API", "126508", "12", "한밭수목원")
		));
		given(placeInternalService.upsertPlaces(request)).willReturn(serviceResponse);

		var response = placeInternalController.upsertPlaces("test-internal-key", request);

		assertThat(response.data()).isEqualTo(serviceResponse);
		verify(placeInternalService).upsertPlaces(request);
	}

	@Test
	void upsertPlaces_fail_whenInternalApiKeyMismatch() {
		InternalPlaceUpsertRequest request = request();

		assertThatThrownBy(() -> placeInternalController.upsertPlaces("wrong-key", request))
			.isInstanceOf(BusinessException.class)
			.extracting(exception -> ((BusinessException) exception).getErrorCode())
			.isEqualTo(SecurityErrorCode.FORBIDDEN);
	}

	@Test
	void upsertPlaces_fail_whenConfiguredInternalApiKeyBlank() {
		PlaceInternalController blankKeyController = new PlaceInternalController(placeInternalService, "");
		InternalPlaceUpsertRequest request = request();

		assertThatThrownBy(() -> blankKeyController.upsertPlaces("", request))
			.isInstanceOf(BusinessException.class)
			.extracting(exception -> ((BusinessException) exception).getErrorCode())
			.isEqualTo(SecurityErrorCode.FORBIDDEN);
	}

	@Test
	void batchGetPlaces_success_returnsBatchGetResponse() {
		InternalPlaceBatchGetRequest request = new InternalPlaceBatchGetRequest(List.of(1L, 2L));
		InternalPlaceBatchGetResponse serviceResponse = new InternalPlaceBatchGetResponse(
			List.of(new InternalPlaceBatchGetResponse.PlaceResponse(
				1L, "한밭수목원", new BigDecimal("36.366"), new BigDecimal("127.388"),
				"TOUR_API", "126508", "12", "대전광역시 서구 둔산대로 169", "3", "1"
			)),
			List.of(2L)
		);
		given(placeInternalService.batchGetPlaces(request)).willReturn(serviceResponse);

		var response = placeInternalController.batchGetPlaces("test-internal-key", request);

		assertThat(response.data()).isEqualTo(serviceResponse);
		verify(placeInternalService).batchGetPlaces(request);
	}

	@Test
	void batchGetPlaces_fail_whenInternalApiKeyMismatch() {
		InternalPlaceBatchGetRequest request = new InternalPlaceBatchGetRequest(List.of(1L));

		assertThatThrownBy(() -> placeInternalController.batchGetPlaces("wrong-key", request))
			.isInstanceOf(BusinessException.class)
			.extracting(exception -> ((BusinessException) exception).getErrorCode())
			.isEqualTo(SecurityErrorCode.FORBIDDEN);
	}

	private InternalPlaceUpsertRequest request() {
		return new InternalPlaceUpsertRequest(List.of(
			new InternalPlaceUpsertRequest.PlaceRequest(
				"TOUR_API",
				"126508",
				"12",
				"NATURE",
				"3",
				"1",
				"한밭수목원",
				"대전광역시 서구 둔산대로 169",
				new BigDecimal("36.366"),
				new BigDecimal("127.388"),
				"https://example.com/place.jpg"
			)
		));
	}
}
