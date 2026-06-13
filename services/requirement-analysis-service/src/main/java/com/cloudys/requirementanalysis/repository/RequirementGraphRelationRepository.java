package com.cloudys.requirementanalysis.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cloudys.requirementanalysis.entity.RequirementGraphRelation;

public interface RequirementGraphRelationRepository extends JpaRepository<RequirementGraphRelation, Long> {

    List<RequirementGraphRelation> findByProjectIdAndSessionIdAndRelationModeAndIsActiveTrue(
            String projectId, String sessionId, String relationMode);

    List<RequirementGraphRelation> findBySnapshotId(String snapshotId);

    List<RequirementGraphRelation> findBySourceReqIdOrTargetReqId(String sourceReqId, String targetReqId);

    List<RequirementGraphRelation> findByProjectIdAndSessionIdAndRelationModeAndSnapshotStatusAndIsActiveTrue(
            String projectId, String sessionId, String relationMode, String snapshotStatus);

    @Modifying
    @Query("UPDATE RequirementGraphRelation r SET r.isActive = false, r.invalidatedAt = :now WHERE r.snapshotId = :snapshotId")
    void invalidateBySnapshotId(@Param("snapshotId") String snapshotId, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE RequirementGraphRelation r SET r.isActive = false, r.invalidatedAt = :now WHERE r.id IN :ids")
    void invalidateByIds(@Param("ids") List<Long> ids, @Param("now") Instant now);
}
