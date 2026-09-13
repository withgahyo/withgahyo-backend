package com.withgahyo.domain.community.repository;

import com.withgahyo.domain.community.entity.CommunityPost;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

	default List<CommunityPost> searchActivePosts(
		String keyword,
		String category,
		String sort,
		Long cursor,
		int limit
	) {
		return searchActivePosts(keyword, category, sort, cursor, PageRequest.of(0, limit));
	}

	@Query("""
		select p
		from CommunityPost p
		join fetch p.author
		where p.deletedAt is null
			and (:cursor is null or p.postId < :cursor)
			and (:category is null or :category = '' or p.category = :category)
			and (
				:keyword is null
				or :keyword = ''
				or lower(p.title) like lower(concat('%', :keyword, '%'))
				or lower(p.content) like lower(concat('%', :keyword, '%'))
			)
		order by
			case when :sort = 'popular' then p.likeCount else 0 end desc,
			p.postId desc
		""")
	List<CommunityPost> searchActivePosts(
		@Param("keyword") String keyword,
		@Param("category") String category,
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
		join fetch p.author
		where p.deletedAt is null
		order by p.likeCount desc, p.postId desc
		""")
	List<CommunityPost> findRecommendedActivePosts(Pageable pageable);
}
