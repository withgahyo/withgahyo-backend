package com.withgahyo.domain.course.service;

import com.withgahyo.domain.course.dto.CourseFamilyMembersResponse;
import com.withgahyo.domain.course.dto.CourseKeywordSuggestionsResponse;
import com.withgahyo.domain.course.repository.CourseInterestKeywordRepository;
import com.withgahyo.domain.family.repository.FamilyRelationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseOptionService {

	private final CourseInterestKeywordRepository courseInterestKeywordRepository;
	private final FamilyRelationRepository familyRelationRepository;

	public CourseOptionService(
		CourseInterestKeywordRepository courseInterestKeywordRepository,
		FamilyRelationRepository familyRelationRepository
	) {
		this.courseInterestKeywordRepository = courseInterestKeywordRepository;
		this.familyRelationRepository = familyRelationRepository;
	}

	@Transactional(readOnly = true)
	public CourseKeywordSuggestionsResponse getKeywordSuggestions() {
		return CourseKeywordSuggestionsResponse.from(
			courseInterestKeywordRepository.findAllByActiveIsTrueOrderByKeywordIdAsc()
		);
	}

	@Transactional(readOnly = true)
	public CourseFamilyMembersResponse getFamilyMembers(Long userId) {
		return CourseFamilyMembersResponse.from(familyRelationRepository.findActiveRelationsByUserId(userId));
	}
}
