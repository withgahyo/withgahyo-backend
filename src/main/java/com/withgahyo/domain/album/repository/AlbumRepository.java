package com.withgahyo.domain.album.repository;

import com.withgahyo.domain.album.entity.Album;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumRepository extends JpaRepository<Album, Long> {

	Optional<Album> findFirstByCourseCourseIdOrderByAlbumIdAsc(Long courseId);
}
