package com.withgahyo.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.withgahyo.domain.user.exception.UserErrorCode;
import com.withgahyo.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
class ProfileImageStorageServiceTest {

	@Mock
	private S3Client r2Client;

	@Test
	void store_success_uploadsToR2AndReturnsPublicUrl() {
		ProfileImageStorageService storageService = new ProfileImageStorageService(
			r2Client, "withgahyo-bucket", "profile-images", "https://images.withgahyo.com"
		);
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"profile.png",
			"image/png",
			"image".getBytes()
		);

		String profileImageUrl = storageService.store(1L, file);

		assertThat(profileImageUrl).startsWith("https://images.withgahyo.com/profile-images/1-");
		assertThat(profileImageUrl).endsWith(".png");
		verify(r2Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
	}

	@Test
	void store_fail_whenFileIsNotImage() {
		ProfileImageStorageService storageService = new ProfileImageStorageService(
			r2Client, "withgahyo-bucket", "profile-images", "https://images.withgahyo.com"
		);
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
