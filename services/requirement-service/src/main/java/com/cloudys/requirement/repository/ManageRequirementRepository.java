package com.cloudys.requirement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cloudys.requirement.entity.ManageRequirement;

public interface ManageRequirementRepository extends JpaRepository<ManageRequirement, String> {

    List<ManageRequirement> findByProjectId(String projectId);

    List<ManageRequirement> findByProjectIdAndDeletedFalse(String projectId);

    List<ManageRequirement> findByProjectIdAndDeletedFalseOrderByOrderIndexAscCreatedAtDesc(String projectId);

    List<ManageRequirement> findByProjectIdOrderByOrderIndexAscCreatedAtDesc(String projectId);

    List<ManageRequirement> findByParentId(String parentId);

    List<ManageRequirement> findBySourceReqId(String sourceReqId);

    Optional<ManageRequirement> findByReqIdAndDeletedFalse(String reqId);

    List<ManageRequirement> findByProjectIdAndSourceReqIdIsNotNullAndDeletedFalse(String projectId);

    List<ManageRequirement> findByProjectIdAndBaselineIdAndDeletedFalse(String projectId, Long baselineId);
}
