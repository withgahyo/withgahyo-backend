package com.withgahyo.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.withgahyo.domain.auth.token.JwtTokenProvider;
import com.withgahyo.domain.user.repository.UserRepository;
import com.withgahyo.global.exception.BusinessException;
import com.withgahyo.global.exception.ErrorResponse;
import com.withgahyo.global.exception.code.SecurityErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtTokenProvider jwtTokenProvider;
	private final UserRepository userRepository;
	private final ObjectMapper objectMapper;

	public JwtAuthenticationFilter(
		JwtTokenProvider jwtTokenProvider,
		UserRepository userRepository,
		ObjectMapper objectMapper
	) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.userRepository = userRepository;
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
		if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
			String accessToken = authorizationHeader.substring(BEARER_PREFIX.length());
			try {
				authenticate(accessToken);
			} catch (BusinessException exception) {
				// 필터 단계 예외는 DispatcherServlet 이전에 발생해 @RestControllerAdvice가 잡지 못하므로,
				// 여기서 직접 나머지 API와 동일한 ErrorResponse 포맷으로 응답한다.
				writeErrorResponse(response, request, exception);
				return;
			}
		}

		filterChain.doFilter(request, response);
	}

	private void authenticate(String accessToken) {
		Long userId = jwtTokenProvider.getUserIdFromAccessToken(accessToken);
		if (!userRepository.existsByUserIdAndDeletedAtIsNull(userId)) {
			throw new BusinessException(SecurityErrorCode.INVALID_TOKEN);
		}
		AuthenticatedUser principal = new AuthenticatedUser(userId);
		UsernamePasswordAuthenticationToken authentication =
			new UsernamePasswordAuthenticationToken(principal, null, List.of());
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	private void writeErrorResponse(
		HttpServletResponse response,
		HttpServletRequest request,
		BusinessException exception
	) throws IOException {
		response.setStatus(exception.getErrorCode().getHttpStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(
			objectMapper.writeValueAsString(ErrorResponse.of(exception.getErrorCode(), request.getRequestURI())));
	}
}
