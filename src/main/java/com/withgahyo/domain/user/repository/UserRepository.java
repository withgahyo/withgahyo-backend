package com.withgahyo.domain.user.repository;

import com.withgahyo.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByProviderAndProviderUserId(String provider, String providerUserId);

	boolean existsByUserIdAndDeletedAtIsNull(Long userId);
}
