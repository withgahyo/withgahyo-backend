package com.withgahyo.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SecurityConfigTest {

	@Test
	void publicEndpoints_includeProfileImageUploads() {
		assertThat(SecurityConfig.publicEndpoints())
			.contains("/uploads/profile-images/**");
	}
}
