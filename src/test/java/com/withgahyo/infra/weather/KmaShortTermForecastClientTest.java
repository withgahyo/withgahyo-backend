package com.withgahyo.infra.weather;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import com.withgahyo.infra.weather.dto.KmaVilageFcstResponse;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class KmaShortTermForecastClientTest {

	private static final String NORMAL_RESPONSE = """
		{
		  "response": {
		    "header": { "resultCode": "00", "resultMsg": "NORMAL_SERVICE" },
		    "body": {
		      "items": {
		        "item": [
		          { "baseDate": "20260921", "baseTime": "0800", "category": "TMP", "fcstDate": "20260921", "fcstTime": "0900", "fcstValue": "23", "nx": 60, "ny": 127 },
		          { "baseDate": "20260921", "baseTime": "0800", "category": "SKY", "fcstDate": "20260921", "fcstTime": "0900", "fcstValue": "1", "nx": 60, "ny": 127 }
		        ]
		      }
		    }
		  }
		}
		""";

	private static final String ERROR_RESULT_CODE_RESPONSE = """
		{
		  "response": {
		    "header": { "resultCode": "22", "resultMsg": "LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR" },
		    "body": null
		  }
		}
		""";

	private static final String EMPTY_ITEMS_RESPONSE = """
		{
		  "response": {
		    "header": { "resultCode": "00", "resultMsg": "NORMAL_SERVICE" },
		    "body": { "items": { "item": [] } }
		  }
		}
		""";

	private HttpServer server;

	@AfterEach
	void tearDown() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	void getShortTermForecast_returnsItems_onNormalResponse() throws IOException {
		WeatherClient client = clientReturning(200, NORMAL_RESPONSE);

		List<KmaVilageFcstResponse.Item> items = client.getShortTermForecast(60, 127);

		assertThat(items).hasSize(2);
		assertThat(items.get(0).category()).isEqualTo("TMP");
		assertThat(items.get(0).fcstValue()).isEqualTo("23");
	}

	@Test
	void getShortTermForecast_returnsEmptyList_whenItemsEmpty() throws IOException {
		WeatherClient client = clientReturning(200, EMPTY_ITEMS_RESPONSE);

		List<KmaVilageFcstResponse.Item> items = client.getShortTermForecast(60, 127);

		assertThat(items).isEmpty();
	}

	@Test
	void getShortTermForecast_throws_whenResultCodeIsNotSuccess() throws IOException {
		WeatherClient client = clientReturning(200, ERROR_RESULT_CODE_RESPONSE);

		assertThatThrownBy(() -> client.getShortTermForecast(60, 127))
			.isInstanceOf(WeatherApiException.class)
			.hasMessageContaining("22");
	}

	@Test
	void getShortTermForecast_throws_onHttp5xx() throws IOException {
		WeatherClient client = clientReturning(500, "internal error");

		assertThatThrownBy(() -> client.getShortTermForecast(60, 127))
			.isInstanceOf(WeatherApiException.class);
	}

	@Test
	void getShortTermForecast_throws_onHttp4xx() throws IOException {
		WeatherClient client = clientReturning(403, "forbidden");

		assertThatThrownBy(() -> client.getShortTermForecast(60, 127))
			.isInstanceOf(WeatherApiException.class);
	}

	@Test
	void getShortTermForecast_throws_onInvalidJson() throws IOException {
		WeatherClient client = clientReturning(200, "<html>not json</html>");

		assertThatThrownBy(() -> client.getShortTermForecast(60, 127))
			.isInstanceOf(WeatherApiException.class);
	}

	@Test
	void getShortTermForecast_throws_whenServiceKeyIsBlank() {
		WeatherClient client = new KmaShortTermForecastClient("http://localhost:1", "");

		assertThatThrownBy(() -> client.getShortTermForecast(60, 127))
			.isInstanceOf(WeatherApiException.class)
			.hasMessageContaining("WEATHER_API_KEY");
	}

	private WeatherClient clientReturning(int status, String body) throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/getVilageFcst", exchange -> {
			exchange.getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
			exchange.sendResponseHeaders(status, bytes.length);
			exchange.getResponseBody().write(bytes);
			exchange.close();
		});
		server.start();
		return new KmaShortTermForecastClient(
			"http://localhost:" + server.getAddress().getPort(),
			"test-weather-key"
		);
	}
}
