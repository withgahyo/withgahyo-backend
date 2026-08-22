package com.withgahyo.domain.family.repository;

import com.withgahyo.domain.family.entity.FamilyRelation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FamilyRelationRepository extends JpaRepository<FamilyRelation, Long> {

	@Query("""
		select fr
		from FamilyRelation fr
		join fetch fr.familyUser
		where fr.user.userId = :userId
			and fr.deletedAt is null
		order by fr.familyRelationId asc
		""")
	List<FamilyRelation> findActiveRelationsByUserId(@Param("userId") Long userId);

	@Query("""
		select fr
		from FamilyRelation fr
		join fetch fr.familyUser
		where fr.user.userId = :userId
			and fr.familyUser.userId in :familyUserIds
			and fr.deletedAt is null
		""")
	List<FamilyRelation> findActiveRelationsByUserIdAndFamilyUserIds(
		@Param("userId") Long userId,
		@Param("familyUserIds") List<Long> familyUserIds
	);
}
