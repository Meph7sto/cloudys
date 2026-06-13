package com.cloudys.requirementanalysis.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cloudys.common.core.exception.ErrorResponse;
import com.cloudys.requirementanalysis.entity.RequirementGraphRelation;
import com.cloudys.requirementanalysis.repository.RequirementGraphRelationRepository;

@Service
public class RequirementGraphService {

    private final RequirementGraphRelationRepository graphRepository;
    private final JsonSupport jsonSupport;

    public RequirementGraphService(RequirementGraphRelationRepository graphRepository, JsonSupport jsonSupport) {
        this.graphRepository = graphRepository;
        this.jsonSupport = jsonSupport;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> searchRelations(String projectId, String sessionId, String relationMode, Boolean activeOnly) {
        List<RequirementGraphRelation> relations;
        if (Boolean.TRUE.equals(activeOnly)) {
            relations = graphRepository.findByProjectIdAndSessionIdAndRelationModeAndIsActiveTrue(
                    projectId, sessionId, relationMode);
        } else {
            relations = graphRepository.findByProjectIdAndSessionIdAndRelationModeAndSnapshotStatusAndIsActiveTrue(
                    projectId, sessionId, relationMode, "final");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("project_id", projectId);
        result.put("session_id", sessionId);
        result.put("relation_mode", relationMode);
        result.put("total", relations.size());
        result.put("relations", relations.stream().map(this::toRelationMap).toList());
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSnapshot(String snapshotId) {
        List<RequirementGraphRelation> relations = graphRepository.findBySnapshotId(snapshotId);
        if (relations.isEmpty()) {
            throw new ErrorResponse("snapshot 不存在: " + snapshotId, 404);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("snapshot_id", snapshotId);
        result.put("total", relations.size());
        result.put("relations", relations.stream().map(this::toRelationMap).toList());
        return result;
    }

    @Transactional
    public Map<String, Object> invalidateRelations(String snapshotId, List<Long> relationIds, String reason) {
        if (relationIds != null && !relationIds.isEmpty()) {
            graphRepository.invalidateByIds(relationIds, Instant.now());
        } else if (snapshotId != null) {
            graphRepository.invalidateBySnapshotId(snapshotId, Instant.now());
        } else {
            throw new ErrorResponse("必须提供 snapshot_id 或 relation_ids", 400);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("invalidated", true);
        result.put("reason", reason);
        result.put("invalidated_at", Instant.now());
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStats(String projectId, String sessionId, String relationMode) {
        List<RequirementGraphRelation> relations = graphRepository
                .findByProjectIdAndSessionIdAndRelationModeAndIsActiveTrue(projectId, sessionId, relationMode);

        Map<String, Long> byType = new LinkedHashMap<>();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        Map<String, Long> byKind = new LinkedHashMap<>();

        for (RequirementGraphRelation r : relations) {
            byType.merge(r.getRelationType(), 1L, Long::sum);
            byStatus.merge(r.getSnapshotStatus(), 1L, Long::sum);
            byKind.merge(r.getSourceKind(), 1L, Long::sum);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total_relations", relations.size());
        result.put("by_relation_type", byType);
        result.put("by_snapshot_status", byStatus);
        result.put("by_source_kind", byKind);
        return result;
    }

    private Map<String, Object> toRelationMap(RequirementGraphRelation r) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", r.getId());
        map.put("snapshot_id", r.getSnapshotId());
        map.put("run_id", r.getRunId());
        map.put("snapshot_status", r.getSnapshotStatus());
        map.put("project_id", r.getProjectId());
        map.put("session_id", r.getSessionId());
        map.put("relation_mode", r.getRelationMode());
        map.put("source_node_id", r.getSourceNodeId());
        map.put("target_node_id", r.getTargetNodeId());
        map.put("source_req_id", r.getSourceReqId());
        map.put("target_req_id", r.getTargetReqId());
        map.put("relation_type", r.getRelationType());
        map.put("source_kind", r.getSourceKind());
        map.put("source_detail", r.getSourceDetail());
        map.put("weight", r.getWeight());
        map.put("confidence", r.getConfidence());
        map.put("reason", r.getReason());
        map.put("evidence", jsonSupport.toMap(r.getEvidence()));
        map.put("model", r.getModel());
        map.put("review_action", r.getReviewAction());
        map.put("is_active", r.getIsActive());
        map.put("created_at", r.getCreatedAt());
        map.put("invalidated_at", r.getInvalidatedAt());
        return map;
    }
}
