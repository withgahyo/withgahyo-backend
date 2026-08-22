package com.withgahyo.global.config;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.withgahyo.global.exception.ErrorResponse;
import com.withgahyo.global.exception.code.SecurityErrorCode;
import org.junit.jupiter.api.Test;

class JacksonConfigTest {

	@Test
	void objectMapper_serializesErrorResponseTimestamp() {
		ObjectMapper objectMapper = new JacksonConfig().objectMapper();
		ErrorResponse errorResponse = ErrorResponse.of(SecurityErrorCode.UNAUTHORIZED, "/api/v1/users/me");

		assertThatCode(() -> objectMapper.writeValueAsString(errorResponse))
			.doesNotThrowAnyException();
	}
}
