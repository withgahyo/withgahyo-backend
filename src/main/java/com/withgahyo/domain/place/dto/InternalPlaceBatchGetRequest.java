package com.withgahyo.domain.place.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record InternalPlaceBatchGetRequest(
	@NotEmpty
	@Size(max = 50, message = "한 번에 조회할 수 있는 장소는 50개까지입니다.")
	List<Long> placeIds
) {
}
