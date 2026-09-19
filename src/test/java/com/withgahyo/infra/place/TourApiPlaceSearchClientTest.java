package com.withgahyo.infra.place;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import com.withgahyo.domain.place.service.ExternalPlaceSearchPage;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TourApiPlaceSearchClientTest {

	private HttpServer server;

	@AfterEach
	void tearDown() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	void search_usesFirstImage_whenFirstImageExists() throws IOException {
		TourApiPlaceSearchClient client = clientReturning(
			itemJson("https://tour.example.com/origin.jpg", "https://tour.example.com/thumb.jpg")
		);

		ExternalPlaceSearchPage page = client.search("3", "1", "수목원", 1, 10);

		assertThat(page.places()).hasSize(1);
		assertThat(page.places().get(0).imageUrl()).isEqualTo("https://tour.example.com/origin.jpg");
	}

	@Test
	void search_fallsBackToFirstImage2_whenFirstImageIsBlank() throws IOException {
		// TourAPI는 원본 이미지가 없을 때 firstimage를 빈 문자열로 내려준다.
		TourApiPlaceSearchClient client = clientReturning(
			itemJson("", "https://tour.example.com/thumb.jpg")
		);

		ExternalPlaceSearchPage page = client.search("3", "1", "수목원", 1, 10);

		assertThat(page.places()).hasSize(1);
		assertThat(page.places().get(0).imageUrl()).isEqualTo("https://tour.example.com/thumb.jpg");
	}

	@Test
	void search_returnsNullImageUrl_whenBothImagesAreBlank() throws IOException {
		TourApiPlaceSearchClient client = clientReturning(itemJson("", ""));

		ExternalPlaceSearchPage page = client.search("3", "1", "수목원", 1, 10);

		assertThat(page.places()).hasSize(1);
		assertThat(page.places().get(0).imageUrl()).isNull();
	}

	@Test
	void search_returnsNullImageUrl_whenImageFieldsAreAbsent() throws IOException {
		TourApiPlaceSearchClient client = clientReturning("""
			{
			  "contentid": "126508",
			  "contenttypeid": "12",
			  "title": "한밭수목원",
			  "addr1": "대전광역시 서구 둔산대로 169",
			  "areacode": "3",
			  "sigungucode": "1",
			  "mapx": "127.3880000",
			  "mapy": "36.3660000"
			}
			""");

		ExternalPlaceSearchPage page = client.search("3", "1", "수목원", 1, 10);

		assertThat(page.places()).hasSize(1);
		assertThat(page.places().get(0).imageUrl()).isNull();
	}

	@Test
	void search_keepsOtherFields_whenImageUrlFallsBack() throws IOException {
		TourApiPlaceSearchClient client = clientReturning(
			itemJson("", "https://tour.example.com/thumb.jpg")
		);

		ExternalPlaceSearchPage page = client.search("3", "1", "수목원", 1, 10);

		var place = page.places().get(0);
		assertThat(place.source()).isEqualTo("TOUR_API");
		assertThat(place.contentTypeId()).isEqualTo("12");
		assertThat(place.externalPlaceId()).isEqualTo("126508");
		assertThat(place.name()).isEqualTo("한밭수목원");
		assertThat(place.category()).isEqualTo("TOURIST_ATTRACTION");
		assertThat(place.address()).isEqualTo("대전광역시 서구 둔산대로 169");
		assertThat(place.areaCode()).isEqualTo("3");
		assertThat(place.sigunguCode()).isEqualTo("1");
		assertThat(place.latitude()).isEqualByComparingTo("36.3660000");
		assertThat(place.longitude()).isEqualByComparingTo("127.3880000");
	}

	private String itemJson(String firstImage, String firstImage2) {
		return """
			{
			  "contentid": "126508",
			  "contenttypeid": "12",
			  "title": "한밭수목원",
			  "addr1": "대전광역시 서구 둔산대로 169",
			  "areacode": "3",
			  "sigungucode": "1",
			  "firstimage": "%s",
			  "firstimage2": "%s",
			  "mapx": "127.3880000",
			  "mapy": "36.3660000"
			}
			""".formatted(firstImage, firstImage2);
	}

	private TourApiPlaceSearchClient clientReturning(String itemJson) throws IOException {
		String responseBody = """
			{
			  "response": {
			    "body": {
			      "totalCount": 1,
			      "items": { "item": [ %s ] }
			    }
			  }
			}
			""".formatted(itemJson);

		server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/searchKeyword2", exchange -> {
			byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
			exchange.sendResponseHeaders(200, bytes.length);
			exchange.getResponseBody().write(bytes);
			exchange.close();
		});
		server.start();

		return new TourApiPlaceSearchClient(
			"http://localhost:" + server.getAddress().getPort(),
			"test-service-key",
			"withgahyo-test"
		);
	}
}
