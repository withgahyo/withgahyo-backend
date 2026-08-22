package com.withgahyo.domain.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.withgahyo.domain.place.dto.PlaceSearchResponse;
import com.withgahyo.domain.place.dto.RegionSearchResponse;
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

@ExtendWith(MockitoExtension.class)
class PlaceSearchServiceTest {

	@Mock
	private RegionRepository regionRepository;

	@Mock
	private PlaceRepository placeRepository;

	@Mock
	private ExternalPlaceSearchClient externalPlaceSearchClient;

	private PlaceSearchService placeSearchService;

	@BeforeEach
	void setUp() {
		PlaceUpsertWriter placeUpsertWriter = new PlaceUpsertWriter(regionRepository, placeRepository);
		placeSearchService = new PlaceSearchService(regionRepository, externalPlaceSearchClient, placeUpsertWriter);
	}

	@Test
	void searchRegions_returnsDisplayNames() {
		Region region = Region.create("3", "1", "대전광역시 동구");
		given(regionRepository.findByNameContaining("대전")).willReturn(List.of(region));

		RegionSearchResponse response = placeSearchService.searchRegions("대전");

		assertThat(response.regions()).hasSize(1);
		assertThat(response.regions().get(0).areaCode()).isEqualTo("3");
		assertThat(response.regions().get(0).sigunguCode()).isEqualTo("1");
		assertThat(response.regions().get(0).parentName()).isEqualTo("대전광역시");
		assertThat(response.regions().get(0).displayName()).isEqualTo("대전 동구");
	}

	@Test
	void searchPlaces_savesExternalResultsAndReturnsNormalizedPlaces() {
		Region region = Region.create("3", "1", "대전광역시 동구");
		ExternalPlaceSearchResult externalPlace = new ExternalPlaceSearchResult(
			"TOUR_API",
			"12",
			"126508",
			"한밭수목원",
			"NATURE",
			"대전광역시 서구 둔산대로 169",
			"3",
			"1",
			"https://example.com/place.jpg",
			new BigDecimal("36.366"),
			new BigDecimal("127.388")
		);
		ExternalPlaceSearchPage externalPage = new ExternalPlaceSearchPage(List.of(externalPlace), false, null);

		given(regionRepository.findByAreaCodeAndSigunguCode("3", "1")).willReturn(Optional.of(region));
		given(externalPlaceSearchClient.search("3", "1", "수목원", null, 10)).willReturn(externalPage);
		given(placeRepository.findByContentIdAndContentTypeId("126508", "12")).willReturn(Optional.empty());
		given(placeRepository.save(org.mockito.ArgumentMatchers.any(Place.class)))
			.willAnswer(invocation -> invocation.getArgument(0));

		PlaceSearchResponse response = placeSearchService.searchPlaces("3", "1", "수목원", null, 10);

		assertThat(response.places()).hasSize(1);
		assertThat(response.places().get(0).source()).isEqualTo("TOUR_API");
		assertThat(response.places().get(0).externalPlaceId()).isEqualTo("126508");
		assertThat(response.places().get(0).areaCode()).isEqualTo("3");
		assertThat(response.hasNext()).isFalse();
		assertThat(response.nextCursor()).isNull();
		verify(placeRepository).save(org.mockito.ArgumentMatchers.any(Place.class));
	}

	@Test
	void searchPlaces_excludesExternalResultsOutsideSelectedRegion() {
		Region region = Region.create("3", "0", "대전");
		ExternalPlaceSearchResult externalPlace = new ExternalPlaceSearchResult(
			"KAKAO",
			"AT4",
			"seoul-place",
			"서울숲",
			"NATURE",
			"서울특별시 성동구 뚝섬로 273",
			"1",
			"0",
			null,
			new BigDecimal("37.544"),
			new BigDecimal("127.037")
		);
		ExternalPlaceSearchPage externalPage = new ExternalPlaceSearchPage(List.of(externalPlace), false, null);

		given(regionRepository.findByAreaCodeAndSigunguCode("3", "0")).willReturn(Optional.of(region));
		given(externalPlaceSearchClient.search("3", "0", "수목원", null, 10)).willReturn(externalPage);

		PlaceSearchResponse response = placeSearchService.searchPlaces("3", "0", "수목원", null, 10);

		assertThat(response.places()).isEmpty();
	}
}
