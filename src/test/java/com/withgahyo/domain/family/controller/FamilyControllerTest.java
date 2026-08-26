package com.withgahyo.domain.family.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.withgahyo.domain.family.dto.FamilyMemberCandidateResponse;
import com.withgahyo.domain.family.dto.FamilyMembersResponse;
import com.withgahyo.domain.family.service.FamilyService;
import com.withgahyo.global.security.AuthenticatedUser;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FamilyControllerTest {

	@Mock
	private FamilyService familyService;

	private FamilyController familyController;

	@BeforeEach
	void setUp() {
		familyController = new FamilyController(familyService);
	}

	@Test
	void getFamilyMembers_returnsMembers_whenEmailIsMissing() {
		FamilyMembersResponse members = new FamilyMembersResponse(List.of());

		given(familyService.getFamilyMembers(1L)).willReturn(members);

		var response = familyController.getFamilyMembers(new AuthenticatedUser(1L), null);

		assertThat(response.data()).isEqualTo(members);
		verify(familyService).getFamilyMembers(1L);
	}

	@Test
	void getFamilyMembers_returnsCandidate_whenEmailIsPresent() {
		FamilyMemberCandidateResponse candidate = new FamilyMemberCandidateResponse(
			2L,
			"엄마",
			null,
			"mo***@example.com",
			false
		);

		given(familyService.findCandidate(1L, "mom@example.com")).willReturn(candidate);

		var response = familyController.getFamilyMembers(new AuthenticatedUser(1L), "mom@example.com");

		assertThat(response.data()).isEqualTo(candidate);
		verify(familyService).findCandidate(1L, "mom@example.com");
	}
}
