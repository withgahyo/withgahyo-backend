package com.withgahyo.domain.auth.repository;

import com.withgahyo.domain.auth.entity.RefreshToken;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	@Modifying
	@Query("""
		update RefreshToken refreshToken
		set refreshToken.revokedAt = :revokedAt
		where refreshToken.user.userId = :userId
			and refreshToken.revokedAt is null
		""")
	int revokeAllByUserId(@Param("userId") Long userId, @Param("revokedAt") LocalDateTime revokedAt);
}
