package com.cloudys.auth.repository;

import com.cloudys.auth.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    List<ProjectMember> findByProjectId(String projectId);

    Optional<ProjectMember> findByProjectIdAndUserId(String projectId, String userId);

    long countByProjectId(String projectId);

    boolean existsByProjectIdAndUserId(String projectId, String userId);

    @Modifying
    @Query("DELETE FROM ProjectMember m WHERE m.projectId = :projectId AND m.userId = :userId")
    void deleteByProjectAndUser(@Param("projectId") String projectId, @Param("userId") String userId);

    @Query("SELECT m.projectId FROM ProjectMember m WHERE m.userId = :userId")
    List<String> findProjectIdsByUserId(@Param("userId") String userId);
}
