package com.withgahyo.domain.community.repository;

import com.withgahyo.domain.community.entity.CommunityComment;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long> {

	default Map<Long, Long> countActiveCommentsByPostIds(List<Long> postIds) {
		if (postIds.isEmpty()) {
			return Map.of();
		}
		return countActiveCommentRowsByPostIds(postIds).stream()
			.collect(Collectors.toMap(CommentCountRow::getPostId, CommentCountRow::getCommentCount));
	}

	@Query("""
		select c.post.postId as postId, count(c.commentId) as commentCount
		from CommunityComment c
		where c.deletedAt is null
			and c.post.postId in :postIds
		group by c.post.postId
		""")
	List<CommentCountRow> countActiveCommentRowsByPostIds(@Param("postIds") List<Long> postIds);

	interface CommentCountRow {
		Long getPostId();

		Long getCommentCount();
	}
}
