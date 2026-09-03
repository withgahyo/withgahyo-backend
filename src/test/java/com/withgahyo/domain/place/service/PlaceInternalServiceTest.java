package com.withgahyo.domain.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

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
		placeInternalService = new PlaceInternalService(placeUpsertWriter);
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
}
