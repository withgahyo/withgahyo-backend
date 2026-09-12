package com.withgahyo.domain.user.service;

import com.withgahyo.domain.user.exception.UserErrorCode;
import com.withgahyo.global.exception.BusinessException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProfileImageStorageService {

	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

	private final Path storagePath;
	private final String publicUrlPrefix;

	@Autowired
	public ProfileImageStorageService(
		@Value("${app.upload.profile-images-dir:uploads/profile-images}") String storagePath,
		@Value("${app.upload.profile-images-url-prefix:/uploads/profile-images}") String publicUrlPrefix
	) {
		this(Path.of(storagePath), publicUrlPrefix);
	}

	ProfileImageStorageService(Path storagePath, String publicUrlPrefix) {
		this.storagePath = storagePath.toAbsolutePath().normalize();
		this.publicUrlPrefix = publicUrlPrefix.endsWith("/")
			? publicUrlPrefix.substring(0, publicUrlPrefix.length() - 1)
			: publicUrlPrefix;
	}

	public String store(Long userId, MultipartFile file) {
		validateImage(file);

		String extension = resolveExtension(file);
		String fileName = "%d-%s%s".formatted(userId, UUID.randomUUID(), extension);
		Path targetPath = storagePath.resolve(fileName).normalize();
		if (!targetPath.startsWith(storagePath)) {
			throw new BusinessException(UserErrorCode.INVALID_PROFILE_IMAGE);
		}

		try {
			Files.createDirectories(storagePath);
			try (InputStream inputStream = file.getInputStream()) {
				Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException exception) {
			throw new BusinessException(UserErrorCode.PROFILE_IMAGE_UPLOAD_FAILED);
		}

		return publicUrlPrefix + "/" + fileName;
	}

	private void validateImage(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BusinessException(UserErrorCode.INVALID_PROFILE_IMAGE);
		}
		if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
			throw new BusinessException(UserErrorCode.INVALID_PROFILE_IMAGE);
		}
	}

	private String resolveExtension(MultipartFile file) {
		String originalFilename = file.getOriginalFilename();
		if (originalFilename == null) {
			return extensionFromContentType(file.getContentType());
		}

		int dotIndex = originalFilename.lastIndexOf('.');
		if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
			return extensionFromContentType(file.getContentType());
		}

		String extension = originalFilename.substring(dotIndex).toLowerCase(Locale.ROOT);
		return switch (extension) {
			case ".jpg", ".jpeg", ".png", ".webp" -> extension;
			default -> extensionFromContentType(file.getContentType());
		};
	}

	private String extensionFromContentType(String contentType) {
		return switch (contentType) {
			case "image/jpeg" -> ".jpg";
			case "image/png" -> ".png";
			case "image/webp" -> ".webp";
			default -> throw new BusinessException(UserErrorCode.INVALID_PROFILE_IMAGE);
		};
	}
}
