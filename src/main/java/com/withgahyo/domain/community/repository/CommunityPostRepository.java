package com.withgahyo.domain.community.repository;

import com.withgahyo.domain.community.entity.CommunityPost;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

	@Query("""
		select p
		from CommunityPost p
		join fetch p.review r
		join fetch r.user
		join fetch r.course c
		join fetch c.region
		where p.postId = :postId
			and p.deletedAt is null
		""")
	Optional<CommunityPost> findActiveById(@Param("postId") Long postId);

	boolean existsByReview_ReviewIdAndDeletedAtIsNull(Long reviewId);

	default List<CommunityPost> searchActivePosts(
		String keyword,
		String regionName,
		String highlightType,
		String sort,
		Long cursor,
		int limit
	) {
		return searchActivePosts(keyword, regionName, highlightType, sort, cursor, PageRequest.of(0, limit));
	}

	@Query("""
		select p
		from CommunityPost p
		join fetch p.review r
		join fetch r.user
		join fetch r.course c
		join fetch c.region
		where p.deletedAt is null
			and (:cursor is null or p.postId < :cursor)
			and (
				:regionName is null or :regionName = ''
				or c.region.name = :regionName
			)
			and (
				:highlightType is null or :highlightType = ''
				or exists (
					select 1 from ReviewHighlight rh
					where rh.review = r and rh.highlightType = :highlightType
				)
			)
			and (
				:keyword is null
				or :keyword = ''
				or lower(c.title) like lower(concat('%', :keyword, '%'))
				or lower(r.comment) like lower(concat('%', :keyword, '%'))
			)
		order by
			case when :sort = 'popular' then p.likeCount else 0 end desc,
			p.postId desc
		""")
	List<CommunityPost> searchActivePosts(
		@Param("keyword") String keyword,
		@Param("regionName") String regionName,
		@Param("highlightType") String highlightType,
		@Param("sort") String sort,
		@Param("cursor") Long cursor,
		Pageable pageable
	);

	default List<CommunityPost> findRecommendedActivePosts(int limit) {
		return findRecommendedActivePosts(PageRequest.of(0, limit));
	}

	@Query("""
		select p
		from CommunityPost p
		join fetch p.review r
		join fetch r.user
		join fetch r.course c
		join fetch c.region
		where p.deletedAt is null
		order by p.likeCount desc, p.postId desc
		""")
	List<CommunityPost> findRecommendedActivePosts(Pageable pageable);
}
