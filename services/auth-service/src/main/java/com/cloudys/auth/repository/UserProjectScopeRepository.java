package com.cloudys.auth.repository;

import com.cloudys.auth.entity.UserProjectScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProjectScopeRepository extends JpaRepository<UserProjectScope, Long> {

    List<UserProjectScope> findByUserId(String userId);

    @Modifying
    @Query("DELETE FROM UserProjectScope s WHERE s.userId = :userId")
    void deleteByUserId(@Param("userId") String userId);

    boolean existsByUserIdAndProjectId(String userId, String projectId);

    @Query("SELECT s.projectId FROM UserProjectScope s WHERE s.userId = :userId")
    List<String> findProjectIdsByUserId(@Param("userId") String userId);

    @Query("SELECT s.canEdit FROM UserProjectScope s WHERE s.userId = :userId AND s.projectId = :projectId")
    Optional<Boolean> findCanEditByUserIdAndProjectId(@Param("userId") String userId,
                                                       @Param("projectId") String projectId);
}
