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
import com.cloudys.requirementanalysis.entity.TraceAnalysis;
import com.cloudys.requirementanalysis.repository.RequirementGraphRelationRepository;
import com.cloudys.requirementanalysis.repository.TraceAnalysisRepository;

@Service
public class RequirementChangeService {

    private final RequirementGraphRelationRepository graphRepository;
    private final TraceAnalysisRepository traceAnalysisRepository;
    private final JsonSupport jsonSupport;

    public RequirementChangeService(RequirementGraphRelationRepository graphRepository,
                                     TraceAnalysisRepository traceAnalysisRepository,
                                     JsonSupport jsonSupport) {
        this.graphRepository = graphRepository;
        this.traceAnalysisRepository = traceAnalysisRepository;
        this.jsonSupport = jsonSupport;
    }

    @Transactional
    public Map<String, Object> analyzeChange(String sessionId, String projectId, List<String> changedReqIds) {
        List<RequirementGraphRelation> affected = new ArrayList<>();
        List<Map<String, Object>> impactDetails = new ArrayList<>();

        for (String reqId : changedReqIds) {
            List<RequirementGraphRelation> sourceRelations = graphRepository.findBySourceReqIdOrTargetReqId(reqId, reqId);
            for (RequirementGraphRelation rel : sourceRelations) {
                if (rel.getIsActive()) {
                    affected.add(rel);
                    Map<String, Object> detail = new LinkedHashMap<>();
                    detail.put("source_req_id", rel.getSourceReqId());
                    detail.put("target_req_id", rel.getTargetReqId());
                    detail.put("relation_type", rel.getRelationType());
                    detail.put("relation_id", rel.getId());
                    boolean isSource = rel.getSourceReqId().equals(reqId);
                    detail.put("impact", isSource ? "outgoing" : "incoming");
                    detail.put("is_direct", rel.getSourceReqId().equals(reqId) || rel.getTargetReqId().equals(reqId));
                    impactDetails.add(detail);
                }
            }
        }

        Map<String, Object> impactSummary = new LinkedHashMap<>();
        impactSummary.put("changed_requirements", changedReqIds.size());
        impactSummary.put("affected_relations", affected.size());
        impactSummary.put("unique_affected_nodes",
                affected.stream().map(r -> {
                    List<String> nodes = new ArrayList<>();
                    if (changedReqIds.contains(r.getSourceReqId())) nodes.add(r.getTargetReqId());
                    if (changedReqIds.contains(r.getTargetReqId())) nodes.add(r.getSourceReqId());
                    return nodes;
                }).flatMap(List::stream).distinct().count());

        TraceAnalysis trace = new TraceAnalysis();
        trace.setSessionId(sessionId);
        trace.setAnalysisType("change_impact");
        trace.setTitle("变更影响分析: " + String.join(", ", changedReqIds));
        trace.setDescription("分析 " + changedReqIds.size() + " 个需求变更对追溯图的影响");
        trace.setInputJson(jsonSupport.toJson(Map.of("changed_req_ids", changedReqIds, "project_id", projectId)));
        trace.setOutputJson(jsonSupport.toJson(Map.of("impact_details", impactDetails, "summary", impactSummary)));
        trace.setCreatedAt(Instant.now());
        traceAnalysisRepository.save(trace);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("analysis_id", trace.getId());
        result.put("session_id", sessionId);
        result.put("project_id", projectId);
        result.put("summary", impactSummary);
        result.put("impact_details", impactDetails);
        result.put("created_at", trace.getCreatedAt());
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getChangeHistory(String sessionId) {
        List<TraceAnalysis> traces = traceAnalysisRepository.findBySessionIdAndAnalysisType(sessionId, "change_impact");
        if (traces.isEmpty()) {
            throw new ErrorResponse("session 不存在变更分析记录: " + sessionId, 404);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("session_id", sessionId);
        result.put("total", traces.size());
        result.put("history", traces.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("title", t.getTitle());
            m.put("description", t.getDescription());
            m.put("input", jsonSupport.toMap(t.getInputJson()));
            m.put("output", jsonSupport.toMap(t.getOutputJson()));
            m.put("created_at", t.getCreatedAt());
            return m;
        }).toList());
        return result;
    }
}
