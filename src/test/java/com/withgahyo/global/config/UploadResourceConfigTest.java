package com.withgahyo.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class UploadResourceConfigTest {

	@Test
	void profileImageResourceLocation_pointsToDirectory() {
		UploadResourceConfig config = new UploadResourceConfig("uploads/profile-images");

		String resourceLocation = config.profileImagesResourceLocation();

		assertThat(resourceLocation)
			.startsWith(Path.of("uploads/profile-images").toAbsolutePath().normalize().toUri().toString())
			.endsWith("/");
	}
}
