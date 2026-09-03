package com.withgahyo.domain.user.service;

import com.withgahyo.domain.auth.repository.RefreshTokenRepository;
import com.withgahyo.domain.user.entity.User;
import com.withgahyo.domain.user.repository.UserRepository;
import com.withgahyo.global.exception.BusinessException;
import com.withgahyo.global.exception.code.SecurityErrorCode;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;

	public UserService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
	}

	@Transactional
	public void withdraw(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(SecurityErrorCode.INVALID_TOKEN));

		LocalDateTime withdrawnAt = LocalDateTime.now();
		user.withdraw(withdrawnAt);
		refreshTokenRepository.revokeAllByUserId(userId, withdrawnAt);
	}
}
