package com.withgahyo.domain.auth.token;

import com.withgahyo.global.exception.BusinessException;
import com.withgahyo.global.exception.code.SecurityErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

	private static final String CLAIM_TOKEN_TYPE = "type";
	private static final String TOKEN_TYPE_ACCESS = "access";
	private static final String TOKEN_TYPE_REFRESH = "refresh";

	private final SecretKey key;
	private final long accessTokenExpiresIn;
	private final long refreshTokenExpiresIn;

	public JwtTokenProvider(
		@Value("${auth.jwt.secret}") String secret,
		@Value("${auth.jwt.access-token-expires-in:3600}") long accessTokenExpiresIn,
		@Value("${auth.jwt.refresh-token-expires-in:1209600}") long refreshTokenExpiresIn
	) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.accessTokenExpiresIn = accessTokenExpiresIn;
		this.refreshTokenExpiresIn = refreshTokenExpiresIn;
	}

	public String createAccessToken(Long userId) {
		return createToken(userId, TOKEN_TYPE_ACCESS, accessTokenExpiresIn);
	}

	public String createRefreshToken(Long userId) {
		return createToken(userId, TOKEN_TYPE_REFRESH, refreshTokenExpiresIn);
	}

	public long getAccessTokenExpiresIn() {
		return accessTokenExpiresIn;
	}

	public LocalDateTime getRefreshTokenExpiresAt() {
		return LocalDateTime.ofInstant(Instant.now().plusSeconds(refreshTokenExpiresIn), ZoneId.systemDefault());
	}

	public Long getUserIdFromAccessToken(String accessToken) {
		return getUserId(accessToken, TOKEN_TYPE_ACCESS);
	}

	public Long getUserIdFromRefreshToken(String refreshToken) {
		return getUserId(refreshToken, TOKEN_TYPE_REFRESH);
	}

	private String createToken(Long userId, String tokenType, long expiresIn) {
		Instant now = Instant.now();
		return Jwts.builder()
			.subject(userId.toString())
			.claim(CLAIM_TOKEN_TYPE, tokenType)
			.issuedAt(Date.from(now))
			.expiration(Date.from(now.plusSeconds(expiresIn)))
			.signWith(key, Jwts.SIG.HS256)
			.compact();
	}

	private Long getUserId(String token, String expectedTokenType) {
		Claims claims = parseClaims(token);
		if (!expectedTokenType.equals(claims.get(CLAIM_TOKEN_TYPE))) {
			throw new BusinessException(SecurityErrorCode.INVALID_TOKEN);
		}
		return Long.valueOf(claims.getSubject());
	}

	private Claims parseClaims(String token) {
		try {
			return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();
		} catch (JwtException | IllegalArgumentException exception) {
			throw new BusinessException(SecurityErrorCode.INVALID_TOKEN);
		}
	}
}
