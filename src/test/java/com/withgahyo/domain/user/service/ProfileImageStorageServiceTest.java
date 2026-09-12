package com.withgahyo.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.withgahyo.domain.user.exception.UserErrorCode;
import com.withgahyo.global.exception.BusinessException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class ProfileImageStorageServiceTest {

	@TempDir
	Path tempDir;

	@Test
	void store_success_savesImageAndReturnsPublicUrl() throws Exception {
		ProfileImageStorageService storageService = new ProfileImageStorageService(tempDir, "/uploads/profile-images");
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"profile.png",
			"image/png",
			"image".getBytes()
		);

		String profileImageUrl = storageService.store(1L, file);

		assertThat(profileImageUrl).startsWith("/uploads/profile-images/1-");
		assertThat(profileImageUrl).endsWith(".png");
		assertThat(Files.exists(tempDir.resolve(Path.of(profileImageUrl).getFileName()))).isTrue();
	}

	@Test
	void store_fail_whenFileIsNotImage() {
		ProfileImageStorageService storageService = new ProfileImageStorageService(tempDir, "/uploads/profile-images");
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"profile.txt",
			"text/plain",
			"text".getBytes()
		);

		assertThatThrownBy(() -> storageService.store(1L, file))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(UserErrorCode.INVALID_PROFILE_IMAGE);
	}
}
