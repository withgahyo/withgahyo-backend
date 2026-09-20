package com.withgahyo.domain.user.service;

import com.withgahyo.domain.user.exception.UserErrorCode;
import com.withgahyo.global.exception.BusinessException;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class ProfileImageStorageService {

	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

	private final S3Client r2Client;
	private final String bucket;
	private final String keyPrefix;
	private final String publicBaseUrl;

	public ProfileImageStorageService(
		S3Client r2Client,
		@Value("${app.r2.bucket}") String bucket,
		@Value("${app.upload.profile-images-key-prefix:profile-images}") String keyPrefix,
		@Value("${app.r2.public-base-url}") String publicBaseUrl
	) {
		this.r2Client = r2Client;
		this.bucket = bucket;
		this.keyPrefix = trimTrailingSlash(keyPrefix);
		this.publicBaseUrl = trimTrailingSlash(publicBaseUrl);
	}

	public String store(Long userId, MultipartFile file) {
		validateImage(file);

		String extension = resolveExtension(file);
		String key = "%s/%d-%s%s".formatted(keyPrefix, userId, UUID.randomUUID(), extension);

		try {
			r2Client.putObject(
				PutObjectRequest.builder()
					.bucket(bucket)
					.key(key)
					.contentType(file.getContentType())
					.contentLength(file.getSize())
					.build(),
				RequestBody.fromInputStream(file.getInputStream(), file.getSize())
			);
		} catch (IOException | S3Exception exception) {
			throw new BusinessException(UserErrorCode.PROFILE_IMAGE_UPLOAD_FAILED);
		}

		return publicBaseUrl + "/" + key;
	}

	private String trimTrailingSlash(String value) {
		return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
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
