package com.withgahyo.domain.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.withgahyo.domain.place.dto.InternalPlaceBatchGetRequest;
import com.withgahyo.domain.place.dto.InternalPlaceBatchGetResponse;
import com.withgahyo.domain.place.dto.InternalPlaceUpsertRequest;
import com.withgahyo.domain.place.dto.InternalPlaceUpsertResponse;
import com.withgahyo.domain.place.entity.Place;
import com.withgahyo.domain.place.entity.Region;
import com.withgahyo.domain.place.repository.PlaceRepository;
import com.withgahyo.domain.place.repository.RegionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PlaceInternalServiceTest {

	@Mock
	private RegionRepository regionRepository;

	@Mock
	private PlaceRepository placeRepository;

	private PlaceInternalService placeInternalService;

	@BeforeEach
	void setUp() {
		PlaceUpsertWriter placeUpsertWriter = new PlaceUpsertWriter(regionRepository, placeRepository);
		placeInternalService = new PlaceInternalService(placeUpsertWriter, placeRepository);
	}

	@Test
	void upsertPlaces_success_returnsSavedPlaceIds() {
		Region region = Region.create("3", "1", "대전광역시 동구");
		InternalPlaceUpsertRequest request = new InternalPlaceUpsertRequest(List.of(
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

		given(regionRepository.findByAreaCodeAndSigunguCode("3", "1")).willReturn(Optional.of(region));
		given(placeRepository.findByContentIdAndContentTypeId("126508", "12")).willReturn(Optional.empty());
		given(placeRepository.save(org.mockito.ArgumentMatchers.any(Place.class)))
			.willAnswer(invocation -> {
				Place place = invocation.getArgument(0);
				ReflectionTestUtils.setField(place, "placeId", 501L);
				return place;
			});

		InternalPlaceUpsertResponse response = placeInternalService.upsertPlaces(request);

		assertThat(response.places()).hasSize(1);
		assertThat(response.places().get(0).placeId()).isEqualTo(501L);
		assertThat(response.places().get(0).source()).isEqualTo("TOUR_API");
		assertThat(response.places().get(0).contentId()).isEqualTo("126508");
		assertThat(response.places().get(0).contentTypeId()).isEqualTo("12");
		assertThat(response.places().get(0).name()).isEqualTo("한밭수목원");
	}

	@Test
	void batchGetPlaces_success_returnsAllRequestedPlaces() {
		Place place1 = placeWithId(1L);
		Place place2 = placeWithId(2L);
		given(placeRepository.findAllByPlaceIdIn(List.of(1L, 2L))).willReturn(List.of(place1, place2));

		InternalPlaceBatchGetResponse response = placeInternalService.batchGetPlaces(
			new InternalPlaceBatchGetRequest(List.of(1L, 2L))
		);

		assertThat(response.places()).hasSize(2);
		assertThat(response.missingPlaceIds()).isEmpty();
	}

	@Test
	void batchGetPlaces_returnsImageUrlAndCategory() {
		// AI 서버 backend_place_client 가 must-visit 장소를 PlaceCandidate 로 만들 때 쓰도록 노출한다.
		Place place = placeWithId(1L);
		given(placeRepository.findAllByPlaceIdIn(List.of(1L))).willReturn(List.of(place));

		InternalPlaceBatchGetResponse response = placeInternalService.batchGetPlaces(
			new InternalPlaceBatchGetRequest(List.of(1L))
		);

		InternalPlaceBatchGetResponse.PlaceResponse placeResponse = response.places().get(0);
		assertThat(placeResponse.imageUrl()).isEqualTo("https://example.com/place.jpg");
		assertThat(placeResponse.category()).isEqualTo("NATURE");
		// 기존 필드도 그대로 유지된다.
		assertThat(placeResponse.placeId()).isEqualTo(1L);
		assertThat(placeResponse.contentId()).isEqualTo("126508");
		assertThat(placeResponse.contentTypeId()).isEqualTo("12");
		assertThat(placeResponse.address()).isEqualTo("대전광역시 서구 둔산대로 169");
		assertThat(placeResponse.areaCode()).isEqualTo("3");
		assertThat(placeResponse.sigunguCode()).isEqualTo("1");
	}

	@Test
	void batchGetPlaces_returnsNullImageUrl_whenPlaceHasNoImage() {
		Place place = placeWithId(1L);
		ReflectionTestUtils.setField(place, "imageUrl", null);
		given(placeRepository.findAllByPlaceIdIn(List.of(1L))).willReturn(List.of(place));

		InternalPlaceBatchGetResponse response = placeInternalService.batchGetPlaces(
			new InternalPlaceBatchGetRequest(List.of(1L))
		);

		assertThat(response.places().get(0).imageUrl()).isNull();
		assertThat(response.places().get(0).category()).isEqualTo("NATURE");
	}

	@Test
	void batchGetPlaces_success_returnsMissingPlaceIds_whenSomeIdsNotFound() {
		Place place1 = placeWithId(1L);
		given(placeRepository.findAllByPlaceIdIn(List.of(1L, 2L))).willReturn(List.of(place1));

		InternalPlaceBatchGetResponse response = placeInternalService.batchGetPlaces(
			new InternalPlaceBatchGetRequest(List.of(1L, 2L))
		);

		assertThat(response.places()).hasSize(1);
		assertThat(response.places().get(0).placeId()).isEqualTo(1L);
		assertThat(response.missingPlaceIds()).containsExactly(2L);
	}

	@Test
	void batchGetPlaces_success_returnsAllMissing_whenNoIdsFound() {
		given(placeRepository.findAllByPlaceIdIn(List.of(1L, 2L))).willReturn(List.of());

		InternalPlaceBatchGetResponse response = placeInternalService.batchGetPlaces(
			new InternalPlaceBatchGetRequest(List.of(1L, 2L))
		);

		assertThat(response.places()).isEmpty();
		assertThat(response.missingPlaceIds()).containsExactly(1L, 2L);
	}

	@Test
	void batchGetPlaces_removesDuplicateIds_beforeQuerying() {
		Place place1 = placeWithId(1L);
		given(placeRepository.findAllByPlaceIdIn(List.of(1L))).willReturn(List.of(place1));

		InternalPlaceBatchGetResponse response = placeInternalService.batchGetPlaces(
			new InternalPlaceBatchGetRequest(List.of(1L, 1L, 1L))
		);

		assertThat(response.places()).hasSize(1);
		assertThat(response.missingPlaceIds()).isEmpty();
		verify(placeRepository).findAllByPlaceIdIn(List.of(1L));
	}

	private Place placeWithId(Long placeId) {
		Region region = Region.create("3", "1", "대전광역시 동구");
		Place place = Place.create(
			"126508",
			"12",
			"TOUR_API",
			"NATURE",
			region,
			"한밭수목원",
			"대전광역시 서구 둔산대로 169",
			new BigDecimal("36.366"),
			new BigDecimal("127.388"),
			"https://example.com/place.jpg"
		);
		ReflectionTestUtils.setField(place, "placeId", placeId);
		return place;
	}
}
