package com.withgahyo.domain.family.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.withgahyo.domain.family.dto.ConnectFamilyMemberRequest;
import com.withgahyo.domain.family.entity.FamilyRelation;
import com.withgahyo.domain.family.exception.FamilyErrorCode;
import com.withgahyo.domain.family.repository.FamilyRelationRepository;
import com.withgahyo.domain.user.entity.User;
import com.withgahyo.domain.user.repository.UserRepository;
import com.withgahyo.global.exception.BusinessException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FamilyServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private FamilyRelationRepository familyRelationRepository;

	private FamilyService familyService;

	@BeforeEach
	void setUp() {
		familyService = new FamilyService(userRepository, familyRelationRepository);
	}

	@Test
	void findCandidate_success_whenUserExistsAndNotConnected() {
		User requester = userWithId(User.create("KAKAO", "me", "me@example.com", "나", null), 1L);
		User candidate = userWithId(User.create("KAKAO", "mom", "mom@example.com", "엄마", "https://example.com/mom.png"), 2L);

		given(userRepository.findById(1L)).willReturn(Optional.of(requester));
		given(userRepository.findActiveUsersByEmail("mom@example.com")).willReturn(List.of(candidate));
		given(familyRelationRepository.existsActiveRelation(1L, 2L)).willReturn(false);

		var response = familyService.findCandidate(1L, "mom@example.com");

		assertThat(response.userId()).isEqualTo(2L);
		assertThat(response.nickname()).isEqualTo("엄마");
		assertThat(response.profileImageUrl()).isEqualTo("https://example.com/mom.png");
		assertThat(response.maskedEmail()).isEqualTo("mo***@example.com");
		assertThat(response.alreadyConnected()).isFalse();
	}

	@Test
	void findCandidate_success_whenAlreadyConnected() {
		User requester = userWithId(User.create("KAKAO", "me", "me@example.com", "나", null), 1L);
		User candidate = userWithId(User.create("KAKAO", "dad", "dad@example.com", "아빠", null), 2L);

		given(userRepository.findById(1L)).willReturn(Optional.of(requester));
		given(userRepository.findActiveUsersByEmail("dad@example.com")).willReturn(List.of(candidate));
		given(familyRelationRepository.existsActiveRelation(1L, 2L)).willReturn(true);

		var response = familyService.findCandidate(1L, "dad@example.com");

		assertThat(response.alreadyConnected()).isTrue();
	}

	@Test
	void findCandidate_fail_whenSelfEmail() {
		User requester = userWithId(User.create("KAKAO", "me", "me@example.com", "나", null), 1L);

		given(userRepository.findById(1L)).willReturn(Optional.of(requester));
		given(userRepository.findActiveUsersByEmail("me@example.com")).willReturn(List.of(requester));

		assertThatThrownBy(() -> familyService.findCandidate(1L, "me@example.com"))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(FamilyErrorCode.SELF_CONNECTION_NOT_ALLOWED)
			);
	}

	@Test
	void findCandidate_fail_whenUserNotFound() {
		given(userRepository.findById(1L)).willReturn(Optional.of(userWithId(User.create("KAKAO", "me", "나", null), 1L)));
		given(userRepository.findActiveUsersByEmail("missing@example.com")).willReturn(List.of());

		assertThatThrownBy(() -> familyService.findCandidate(1L, "missing@example.com"))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(FamilyErrorCode.FAMILY_USER_NOT_FOUND)
			);
	}

	@Test
	void findCandidate_fail_whenEmailMatchesMultipleUsers() {
		User requester = userWithId(User.create("KAKAO", "me", "me@example.com", "나", null), 1L);
		User kakaoUser = userWithId(User.create("KAKAO", "target", "target@example.com", "가족", null), 2L);
		User googleUser = userWithId(User.create("GOOGLE", "target", "target@example.com", "가족", null), 3L);

		given(userRepository.findById(1L)).willReturn(Optional.of(requester));
		given(userRepository.findActiveUsersByEmail("target@example.com")).willReturn(List.of(kakaoUser, googleUser));

		assertThatThrownBy(() -> familyService.findCandidate(1L, "target@example.com"))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(FamilyErrorCode.EMAIL_MATCHES_MULTIPLE_USERS)
			);
	}

	@Test
	void connect_success_createsRelation() {
		User requester = userWithId(User.create("KAKAO", "me", "나", null), 1L);
		User familyUser = userWithId(User.create("KAKAO", "sister", "동생", "https://example.com/sister.png"), 2L);

		given(userRepository.findById(1L)).willReturn(Optional.of(requester));
		given(userRepository.findById(2L)).willReturn(Optional.of(familyUser));
		given(familyRelationRepository.findRelation(1L, 2L)).willReturn(Optional.empty());
		given(familyRelationRepository.save(any(FamilyRelation.class))).willAnswer(invocation -> invocation.getArgument(0));

		var response = familyService.connect(1L, new ConnectFamilyMemberRequest(2L, "동생"));

		assertThat(response.familyMemberId()).isEqualTo(2L);
		assertThat(response.nickname()).isEqualTo("동생");
		assertThat(response.relationship()).isEqualTo("동생");
		assertThat(response.profileImageUrl()).isEqualTo("https://example.com/sister.png");

		ArgumentCaptor<FamilyRelation> captor = ArgumentCaptor.forClass(FamilyRelation.class);
		verify(familyRelationRepository).save(captor.capture());
		assertThat(captor.getValue().getUser()).isEqualTo(requester);
		assertThat(captor.getValue().getFamilyUser()).isEqualTo(familyUser);
	}

	@Test
	void connect_fail_whenAlreadyConnected() {
		User requester = userWithId(User.create("KAKAO", "me", "나", null), 1L);
		User familyUser = userWithId(User.create("KAKAO", "sister", "동생", null), 2L);
		FamilyRelation relation = FamilyRelation.create(requester, familyUser, "동생");

		given(userRepository.findById(1L)).willReturn(Optional.of(requester));
		given(userRepository.findById(2L)).willReturn(Optional.of(familyUser));
		given(familyRelationRepository.findRelation(1L, 2L)).willReturn(Optional.of(relation));

		assertThatThrownBy(() -> familyService.connect(1L, new ConnectFamilyMemberRequest(2L, "동생")))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(FamilyErrorCode.ALREADY_CONNECTED)
			);
	}

	private User userWithId(User user, Long userId) {
		try {
			Field field = User.class.getDeclaredField("userId");
			field.setAccessible(true);
			field.set(user, userId);
			return user;
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
