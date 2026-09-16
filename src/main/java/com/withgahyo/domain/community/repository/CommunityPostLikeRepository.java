package com.withgahyo.domain.community.repository;

import com.withgahyo.domain.community.entity.CommunityPostLike;
import com.withgahyo.domain.community.entity.CommunityPostLikeId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityPostLikeRepository extends JpaRepository<CommunityPostLike, CommunityPostLikeId> {

	@Modifying
	@Query(
		value = """
			insert ignore into community_post_like (user_id, post_id, created_at)
			values (:userId, :postId, current_timestamp)
			""",
		nativeQuery = true
	)
	int insertIgnore(@Param("userId") Long userId, @Param("postId") Long postId);

	@Modifying
	@Query("""
		delete from CommunityPostLike l
		where l.id.userId = :userId
			and l.id.postId = :postId
		""")
	int deleteByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);

	@Query("""
		select l.id.postId
		from CommunityPostLike l
		where l.id.userId = :userId
			and l.id.postId in :postIds
		""")
	List<Long> findLikedPostIds(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);
}
