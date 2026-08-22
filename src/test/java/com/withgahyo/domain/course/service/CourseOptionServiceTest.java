package com.withgahyo.domain.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.withgahyo.domain.course.dto.CourseKeywordSuggestionsResponse;
import com.withgahyo.domain.course.entity.CourseInterestKeyword;
import com.withgahyo.domain.course.repository.CourseInterestKeywordRepository;
import com.withgahyo.domain.family.entity.FamilyRelation;
import com.withgahyo.domain.family.repository.FamilyRelationRepository;
import com.withgahyo.domain.user.entity.User;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseOptionServiceTest {

	@Mock
	private CourseInterestKeywordRepository courseInterestKeywordRepository;

	@Mock
	private FamilyRelationRepository familyRelationRepository;

	private CourseOptionService courseOptionService;

	@BeforeEach
	void setUp() {
		courseOptionService = new CourseOptionService(courseInterestKeywordRepository, familyRelationRepository);
	}

	@Test
	void getKeywordSuggestions_returnsActiveKeywords() {
		given(courseInterestKeywordRepository.findAllByActiveIsTrueOrderByKeywordIdAsc())
			.willReturn(List.of(CourseInterestKeyword.create("NATURE", "자연")));

		CourseKeywordSuggestionsResponse response = courseOptionService.getKeywordSuggestions();

		assertThat(response.keywords()).hasSize(1);
		assertThat(response.keywords().get(0).code()).isEqualTo("NATURE");
		assertThat(response.keywords().get(0).name()).isEqualTo("자연");
	}

	@Test
	void getFamilyMembers_returnsActiveRelations() {
		User user = User.create("KAKAO", "creator", "작성자", null);
		User familyUser = User.create("KAKAO", "family", "아빠", "https://example.com/dad.png");
		FamilyRelation relation = FamilyRelation.create(user, familyUser, "아빠");
		given(familyRelationRepository.findActiveRelationsByUserId(1L)).willReturn(List.of(relation));

		var response = courseOptionService.getFamilyMembers(1L);

		assertThat(response.familyMembers()).hasSize(1);
		assertThat(response.familyMembers().get(0).nickname()).isEqualTo("아빠");
		assertThat(response.familyMembers().get(0).relationship()).isEqualTo("아빠");
	}
}
