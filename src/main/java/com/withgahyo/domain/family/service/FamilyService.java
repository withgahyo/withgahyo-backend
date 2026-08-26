package com.withgahyo.domain.family.service;

import com.withgahyo.domain.family.dto.ConnectFamilyMemberRequest;
import com.withgahyo.domain.family.dto.FamilyMemberCandidateResponse;
import com.withgahyo.domain.family.dto.FamilyMemberResponse;
import com.withgahyo.domain.family.dto.FamilyMembersResponse;
import com.withgahyo.domain.family.entity.FamilyRelation;
import com.withgahyo.domain.family.exception.FamilyErrorCode;
import com.withgahyo.domain.family.repository.FamilyRelationRepository;
import com.withgahyo.domain.user.entity.User;
import com.withgahyo.domain.user.repository.UserRepository;
import com.withgahyo.global.exception.BusinessException;
import com.withgahyo.global.exception.code.SecurityErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FamilyService {

	private final UserRepository userRepository;
	private final FamilyRelationRepository familyRelationRepository;

	public FamilyService(UserRepository userRepository, FamilyRelationRepository familyRelationRepository) {
		this.userRepository = userRepository;
		this.familyRelationRepository = familyRelationRepository;
	}

	@Transactional(readOnly = true)
	public FamilyMembersResponse getFamilyMembers(Long userId) {
		return FamilyMembersResponse.from(familyRelationRepository.findActiveRelationsByUserId(userId));
	}

	@Transactional(readOnly = true)
	public FamilyMemberCandidateResponse findCandidate(Long userId, String email) {
		User requester = findActiveUser(userId);
		User candidate = findCandidateByEmail(email);

		validateNotSelf(requester, candidate);

		boolean alreadyConnected = familyRelationRepository.existsActiveRelation(userId, candidate.getUserId());
		return FamilyMemberCandidateResponse.of(candidate, alreadyConnected);
	}

	@Transactional
	public FamilyMemberResponse connect(Long userId, ConnectFamilyMemberRequest request) {
		User requester = findActiveUser(userId);
		User familyUser = userRepository.findById(request.familyUserId())
			.filter(user -> user.getDeletedAt() == null)
			.orElseThrow(() -> new BusinessException(FamilyErrorCode.FAMILY_USER_NOT_FOUND));

		validateNotSelf(requester, familyUser);

		FamilyRelation relation = familyRelationRepository.findRelation(userId, familyUser.getUserId())
			.map(existingRelation -> restoreRelation(existingRelation, request.relationship()))
			.orElseGet(() -> familyRelationRepository.save(
				FamilyRelation.create(requester, familyUser, request.relationship())
			));

		return FamilyMemberResponse.from(relation);
	}

	@Transactional
	public void disconnect(Long userId, Long familyMemberId) {
		FamilyRelation relation = familyRelationRepository.findRelation(userId, familyMemberId)
			.filter(FamilyRelation::isActive)
			.orElseThrow(() -> new BusinessException(FamilyErrorCode.FAMILY_RELATION_NOT_FOUND));

		relation.disconnect();
	}

	private User findActiveUser(Long userId) {
		return userRepository.findById(userId)
			.filter(user -> user.getDeletedAt() == null)
			.orElseThrow(() -> new BusinessException(SecurityErrorCode.INVALID_TOKEN));
	}

	private User findCandidateByEmail(String email) {
		List<User> users = userRepository.findActiveUsersByEmail(email.trim());
		if (users.isEmpty()) {
			throw new BusinessException(FamilyErrorCode.FAMILY_USER_NOT_FOUND);
		}
		if (users.size() > 1) {
			throw new BusinessException(FamilyErrorCode.EMAIL_MATCHES_MULTIPLE_USERS);
		}
		return users.get(0);
	}

	private void validateNotSelf(User requester, User candidate) {
		if (requester.getUserId().equals(candidate.getUserId())) {
			throw new BusinessException(FamilyErrorCode.SELF_CONNECTION_NOT_ALLOWED);
		}
	}

	private FamilyRelation restoreRelation(FamilyRelation relation, String relationship) {
		if (relation.isActive()) {
			throw new BusinessException(FamilyErrorCode.ALREADY_CONNECTED);
		}
		relation.restore(relationship);
		return relation;
	}
}
