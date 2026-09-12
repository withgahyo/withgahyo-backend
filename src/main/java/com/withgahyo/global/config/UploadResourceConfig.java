package com.withgahyo.global.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class UploadResourceConfig implements WebMvcConfigurer {

	private final Path profileImagesPath;

	public UploadResourceConfig(
		@Value("${app.upload.profile-images-dir:uploads/profile-images}") String profileImagesPath
	) {
		this.profileImagesPath = Path.of(profileImagesPath).toAbsolutePath().normalize();
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/uploads/profile-images/**")
			.addResourceLocations(profileImagesResourceLocation());
	}

	String profileImagesResourceLocation() {
		String resourceLocation = profileImagesPath.toUri().toString();
		return resourceLocation.endsWith("/") ? resourceLocation : resourceLocation + "/";
	}
}
