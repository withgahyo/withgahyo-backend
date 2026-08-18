package com.withgahyo.domain.auth.token;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.withgahyo.global.exception.BusinessException;
import com.withgahyo.global.exception.code.SecurityErrorCode;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final String TOKEN_TYPE_ACCESS = "access";
	private static final String TOKEN_TYPE_REFRESH = "refresh";

	private final ObjectMapper objectMapper;
	private final byte[] secret;
	private final long accessTokenExpiresIn;
	private final long refreshTokenExpiresIn;

	public JwtTokenProvider(
		ObjectMapper objectMapper,
		@Value("${auth.jwt.secret}") String secret,
		@Value("${auth.jwt.access-token-expires-in:3600}") long accessTokenExpiresIn,
		@Value("${auth.jwt.refresh-token-expires-in:1209600}") long refreshTokenExpiresIn
	) {
		this.objectMapper = objectMapper;
		this.secret = secret.getBytes(StandardCharsets.UTF_8);
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
		Map<String, Object> claims = parseClaims(accessToken);
		validateTokenType(claims, TOKEN_TYPE_ACCESS);
		validateExpiration(claims);
		return Long.valueOf(claims.get("sub").toString());
	}

	public Long getUserIdFromRefreshToken(String refreshToken) {
		Map<String, Object> claims = parseClaims(refreshToken);
		validateTokenType(claims, TOKEN_TYPE_REFRESH);
		validateExpiration(claims);
		return Long.valueOf(claims.get("sub").toString());
	}

	private String createToken(Long userId, String tokenType, long expiresIn) {
		long now = Instant.now().getEpochSecond();
		Map<String, Object> header = Map.of(
			"alg", "HS256",
			"typ", "JWT"
		);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("sub", userId.toString());
		payload.put("type", tokenType);
		payload.put("iat", now);
		payload.put("exp", now + expiresIn);

		String encodedHeader = encodeJson(header);
		String encodedPayload = encodeJson(payload);
		String unsignedToken = encodedHeader + "." + encodedPayload;
		return unsignedToken + "." + sign(unsignedToken);
	}

	private String encodeJson(Map<String, Object> value) {
		try {
			return base64UrlEncode(objectMapper.writeValueAsBytes(value));
		} catch (JsonProcessingException exception) {
			throw new BusinessException(SecurityErrorCode.INVALID_TOKEN);
		}
	}

	private String sign(String unsignedToken) {
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
			return base64UrlEncode(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException | InvalidKeyException exception) {
			throw new BusinessException(SecurityErrorCode.INVALID_TOKEN);
		}
	}

	private String base64UrlEncode(byte[] value) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
	}

	private Map<String, Object> parseClaims(String token) {
		String[] parts = token.split("\\.");
		if (parts.length != 3) {
			throw new BusinessException(SecurityErrorCode.INVALID_TOKEN);
		}
		String unsignedToken = parts[0] + "." + parts[1];
		if (!sign(unsignedToken).equals(parts[2])) {
			throw new BusinessException(SecurityErrorCode.INVALID_TOKEN);
		}
		try {
			return objectMapper.readValue(Base64.getUrlDecoder().decode(parts[1]), new TypeReference<>() {
			});
		} catch (IllegalArgumentException | IOException exception) {
			throw new BusinessException(SecurityErrorCode.INVALID_TOKEN);
		}
	}

	private void validateTokenType(Map<String, Object> claims, String expectedTokenType) {
		if (!expectedTokenType.equals(claims.get("type"))) {
			throw new BusinessException(SecurityErrorCode.INVALID_TOKEN);
		}
	}

	private void validateExpiration(Map<String, Object> claims) {
		Object expiration = claims.get("exp");
		if (!(expiration instanceof Number number) || number.longValue() <= Instant.now().getEpochSecond()) {
			throw new BusinessException(SecurityErrorCode.INVALID_TOKEN);
		}
	}
}
