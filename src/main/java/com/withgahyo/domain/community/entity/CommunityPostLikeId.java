package com.withgahyo.domain.community.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
public class CommunityPostLikeId implements Serializable {

	@Column(name = "user_id")
	private Long userId;

	@Column(name = "post_id")
	private Long postId;

	public static CommunityPostLikeId of(Long userId, Long postId) {
		CommunityPostLikeId id = new CommunityPostLikeId();
		id.userId = userId;
		id.postId = postId;
		return id;
	}
}
