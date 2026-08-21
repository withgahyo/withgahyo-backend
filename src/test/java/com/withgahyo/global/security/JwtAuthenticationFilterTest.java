package com.withgahyo.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.withgahyo.domain.auth.token.JwtTokenProvider;
import com.withgahyo.domain.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

	private static final String SECRET = "test-secret-for-auth-tests-minimum-32-bytes-long";

	@Mock
	private UserRepository userRepository;

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	@Mock
	private FilterChain filterChain;

	private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

	@Test
	void doFilter_returns401WithErrorResponseBody_whenAccessTokenIsExpired() throws Exception {
		JwtTokenProvider expiredTokenIssuer = new JwtTokenProvider(SECRET, -10, 1209600);
		String expiredAccessToken = expiredTokenIssuer.createAccessToken(1L);
		JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(SECRET, 3600, 1209600);

		assertUnauthorizedWithErrorResponse(expiredAccessToken, jwtTokenProvider);
	}

	@Test
	void doFilter_returns401WithErrorResponseBody_whenAccessTokenIsTampered() throws Exception {
		JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(SECRET, 3600, 1209600);
		String validAccessToken = jwtTokenProvider.createAccessToken(1L);
		String tamperedAccessToken = tamperLastCharacter(validAccessToken);

		assertUnauthorizedWithErrorResponse(tamperedAccessToken, jwtTokenProvider);
	}

	private void assertUnauthorizedWithErrorResponse(String accessToken, JwtTokenProvider jwtTokenProvider)
		throws Exception {
		JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, userRepository, objectMapper);
		given(request.getHeader("Authorization")).willReturn("Bearer " + accessToken);
		given(request.getRequestURI()).willReturn("/api/v1/users/me");
		StringWriter body = new StringWriter();
		given(response.getWriter()).willReturn(new PrintWriter(body));

		filter.doFilter(request, response, filterChain);

		verify(response).setStatus(401);
		verify(filterChain, never()).doFilter(request, response);

		String json = body.toString();
		assertThat(json).contains("\"success\":false");
		assertThat(json).contains("\"status\":401");
		assertThat(json).contains("\"code\":\"AUTH_401_002\"");
		assertThat(json).contains("\"path\":\"/api/v1/users/me\"");
	}

	private String tamperLastCharacter(String token) {
		char lastChar = token.charAt(token.length() - 1);
		char replacement = lastChar == 'a' ? 'b' : 'a';
		return token.substring(0, token.length() - 1) + replacement;
	}
}
