package com.withgahyo.domain.user.repository;

import com.withgahyo.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderUserId(String provider, String providerUserId);

    @Query("""
       select u
       from User u
       where lower(u.email) = lower(:email)
          and u.deletedAt is null
       """)
    List<User> findActiveUsersByEmail(@Param("email") String email);

    boolean existsByUserIdAndDeletedAtIsNull(Long userId);
}