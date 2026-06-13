package com.cloudys.auth.repository;

import com.cloudys.auth.entity.MemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberRoleRepository extends JpaRepository<MemberRole, Long> {

    List<MemberRole> findByMemberId(Long memberId);

    @Query("SELECT r.roleId FROM MemberRole r WHERE r.memberId = :memberId")
    List<String> findRoleIdsByMemberId(@Param("memberId") Long memberId);

    boolean existsByMemberIdAndRoleId(Long memberId, String roleId);

    @Modifying
    @Query("DELETE FROM MemberRole r WHERE r.memberId = :memberId AND r.roleId = :roleId")
    void deleteByMemberAndRole(@Param("memberId") Long memberId, @Param("roleId") String roleId);

    @Modifying
    @Query("DELETE FROM MemberRole r WHERE r.memberId = :memberId")
    void deleteAllByMemberId(@Param("memberId") Long memberId);
}
