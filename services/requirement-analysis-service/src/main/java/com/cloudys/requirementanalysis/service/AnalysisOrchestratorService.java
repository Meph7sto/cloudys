package com.cloudys.requirementanalysis.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cloudys.common.core.exception.ErrorResponse;
import com.cloudys.requirementanalysis.client.InferenceServiceClient;
import com.cloudys.requirementanalysis.client.RequirementServiceClient;
import com.cloudys.requirementanalysis.dto.RequirementImportRequest;
import com.cloudys.requirementanalysis.entity.ClassificationAnalysis;
import com.cloudys.requirementanalysis.entity.ConflictAnalysis;
import com.cloudys.requirementanalysis.entity.LowLevelRequirement;
import com.cloudys.requirementanalysis.entity.LowLevelRequirementLink;
import com.cloudys.requirementanalysis.entity.RequirementGraphRelation;
import com.cloudys.requirementanalysis.entity.RequirementL123;
import com.cloudys.requirementanalysis.entity.RequirementsAnalysisRun;
import com.cloudys.requirementanalysis.repository.ClassificationAnalysisRepository;
import com.cloudys.requirementanalysis.repository.ConflictAnalysisRepository;
import com.cloudys.requirementanalysis.repository.LowLevelRequirementLinkRepository;
import com.cloudys.requirementanalysis.repository.LowLevelRequirementRepository;
import com.cloudys.requirementanalysis.repository.RequirementGraphRelationRepository;
import com.cloudys.requirementanalysis.repository.RequirementL123Repository;
import com.cloudys.requirementanalysis.repository.RequirementsAnalysisRunRepository;

@Service
public class AnalysisOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisOrchestratorService.class);

    private final InferenceServiceClient inferenceServiceClient;
    private final RequirementServiceClient requirementServiceClient;
    private final ProgressEmitterService progressEmitter;
    private final JsonSupport jsonSupport;
    private final RequirementsAnalysisRunRepository runRepository;
    private final RequirementL123Repository l123Repository;
    private final LowLevelRequirementRepository lowLevelRepository;
    private final LowLevelRequirementLinkRepository linkRepository;
    private final ClassificationAnalysisRepository classificationRepository;
    private final ConflictAnalysisRepository conflictRepository;
    private final RequirementGraphRelationRepository graphRepository;

    public AnalysisOrchestratorService(InferenceServiceClient inferenceServiceClient,
                                        RequirementServiceClient requirementServiceClient,
                                        ProgressEmitterService progressEmitter,
                                        JsonSupport jsonSupport,
                                        RequirementsAnalysisRunRepository runRepository,
                                        RequirementL123Repository l123Repository,
                                        LowLevelRequirementRepository lowLevelRepository,
                                        LowLevelRequirementLinkRepository linkRepository,
                                        ClassificationAnalysisRepository classificationRepository,
                                        ConflictAnalysisRepository conflictRepository,
                                        RequirementGraphRelationRepository graphRepository) {
        this.inferenceServiceClient = inferenceServiceClient;
        this.requirementServiceClient = requirementServiceClient;
        this.progressEmitter = progressEmitter;
        this.jsonSupport = jsonSupport;
        this.runRepository = runRepository;
        this.l123Repository = l123Repository;
        this.lowLevelRepository = lowLevelRepository;
        this.linkRepository = linkRepository;
        this.classificationRepository = classificationRepository;
        this.conflictRepository = conflictRepository;
        this.graphRepository = graphRepository;
    }

    @Transactional
    public Map<String, Object> startAnalysis(String sessionId, String projectId, String contextRunId) {
        String runId = UUID.randomUUID().toString();
        RequirementsAnalysisRun run = new RequirementsAnalysisRun();
        run.setAnalysisRunId(runId);
        run.setSessionId(sessionId);
        run.setProjectId(projectId);
        run.setContextRunId(contextRunId);
        run.setCreatedAt(Instant.now());
        runRepository.save(run);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("analysis_run_id", runId);
        result.put("status", "RUNNING");
        return result;
    }

    @Async
    @Transactional
    public void executeAnalysis(String runId,
                                String sessionId,
                                String projectId,
                                String contextRunId,
                                Boolean autoImport,
                                String mappingMode) {
        Map<String, Object> finalResult = new LinkedHashMap<>();
        try {
            // Step 1: Extract L1/L2/L3 requirements
            progressEmitter.sendProgress(runId, "extraction", 10, "正在提取需求 (L1/L2/L3)...");
            stepExtract(runId, sessionId, contextRunId);

            // Step 2: Generate L4 from L3
            progressEmitter.sendProgress(runId, "l4_generation", 30, "正在生成 L4 详细需求...");
            stepGenerateL4(runId, sessionId);

            // Step 3: Classify
            progressEmitter.sendProgress(runId, "classification", 50, "正在对需求进行分类...");
            stepClassify(runId, sessionId);

            // Step 4: Dedup
            progressEmitter.sendProgress(runId, "dedup", 65, "正在检测重复需求...");
            stepDedup(runId, sessionId);

            // Step 5: Trace relations
            progressEmitter.sendProgress(runId, "traceability", 80, "正在构建需求追溯图谱...");
            stepTrace(runId, sessionId, projectId);

            // Step 6: Conflict detection
            progressEmitter.sendProgress(runId, "conflict", 90, "正在检测需求冲突...");
            stepConflict(runId, sessionId);

            boolean shouldImport = autoImport == null || autoImport;
            if (shouldImport) {
                progressEmitter.sendProgress(runId, "requirement_import", 95, "正在写入需求管理服务...");
                stepImport(runId, sessionId, projectId, mappingMode);
            }

            progressEmitter.sendProgress(runId, "complete", 100, "全部分析完成");

            finalResult.put("status", "COMPLETED");
            finalResult.put("analysis_run_id", runId);
            finalResult.put("auto_import", shouldImport);
            progressEmitter.sendComplete(runId, finalResult);
        } catch (Exception e) {
            log.error("Analysis pipeline failed for run {}: {}", runId, e.getMessage(), e);
            progressEmitter.sendError(runId, e.getMessage());
        }
    }

    private void stepExtract(String runId, String sessionId, String contextRunId) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("session_id", sessionId);
        request.put("context_run_id", contextRunId);
        Map<String, Object> response = inferenceServiceClient.extractRequirements(request);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> requirements = (List<Map<String, Object>>) response.getOrDefault("requirements", List.of());
        List<Map<String, Object>> highLevel = new ArrayList<>();

        for (Map<String, Object> req : requirements) {
            RequirementL123 entity = new RequirementL123();
            String reqId = UUID.randomUUID().toString();
            entity.setReqId(reqId);
            entity.setSessionId(sessionId);
            entity.setLevel((String) req.getOrDefault("level", "L3"));
            entity.setText((String) req.getOrDefault("text", ""));
            entity.setFingerprint((String) req.getOrDefault("fingerprint", reqId));
            entity.setAnchorSpanId((String) req.getOrDefault("anchor_span_id", null));
            entity.setCreatedAt(Instant.now());
            l123Repository.save(entity);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("req_id", reqId);
            item.put("level", entity.getLevel());
            item.put("text", entity.getText());
            highLevel.add(item);
        }

        updateRunResult(runId, "high_level_requirements", highLevel);
    }

    private void stepGenerateL4(String runId, String sessionId) {
        List<RequirementL123> l3Requirements = l123Repository.findBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream().filter(r -> "L3".equals(r.getLevel())).toList();

        List<Map<String, Object>> lowLevel = new ArrayList<>();
        for (RequirementL123 l3 : l3Requirements) {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("session_id", sessionId);
            request.put("source_top_id", l3.getReqId());
            request.put("source_top_text", l3.getText());

            try {
                Map<String, Object> response = inferenceServiceClient.generateL4(request);

                LowLevelRequirement low = new LowLevelRequirement();
                String lowId = UUID.randomUUID().toString();
                low.setReqId(lowId);
                low.setSessionId(sessionId);
                low.setSourceTopId(l3.getReqId());
                low.setSourceTopText(l3.getText());
                low.setText((String) response.getOrDefault("text", ""));
                low.setComponent((String) response.getOrDefault("component", ""));
                low.setAcceptanceCriteria(jsonSupport.toJson(response.get("acceptance_criteria")));
                low.setTestMethod((String) response.getOrDefault("test_method", ""));
                low.setCreatedAt(Instant.now());
                lowLevelRepository.save(low);

                LowLevelRequirementLink link = new LowLevelRequirementLink();
                link.setReqId(lowId);
                link.setSessionId(sessionId);
                link.setTopReqId(l3.getReqId());
                link.setCreatedAt(Instant.now());
                linkRepository.save(link);

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("req_id", lowId);
                item.put("source_top_id", l3.getReqId());
                item.put("text", low.getText());
                lowLevel.add(item);
            } catch (Exception e) {
                log.warn("L4 generation failed for L3 {}: {}", l3.getReqId(), e.getMessage());
            }
        }

        updateRunResult(runId, "low_level_requirements", lowLevel);
    }

    private void stepClassify(String runId, String sessionId) {
        List<RequirementL123> all = l123Repository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        List<String> texts = all.stream().map(RequirementL123::getText).toList();

        if (texts.isEmpty()) {
            return;
        }

        Map<String, Object> request = Map.of("requirements", texts, "max_length", 512);
        Map<String, Object> response = inferenceServiceClient.classifyTexts(request);

        ClassificationAnalysis analysis = new ClassificationAnalysis();
        analysis.setSessionId(sessionId);
        analysis.setRequirements(jsonSupport.toJson(texts));
        analysis.setPredictions(jsonSupport.toJson(response.get("predictions")));
        analysis.setLabelDistribution(jsonSupport.toJson(response.get("label_distribution")));
        analysis.setTotal(texts.size());
        analysis.setResultJson(jsonSupport.toJson(response));
        analysis.setCreatedAt(Instant.now());
        classificationRepository.save(analysis);

        updateRunResult(runId, "classification_result", response);
    }

    private void stepDedup(String runId, String sessionId) {
        List<RequirementL123> all = l123Repository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        Map<String, List<String>> byFingerprint = new LinkedHashMap<>();
        for (RequirementL123 req : all) {
            byFingerprint.computeIfAbsent(req.getFingerprint(), k -> new ArrayList<>()).add(req.getReqId());
        }
        List<Map<String, Object>> dupGroups = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : byFingerprint.entrySet()) {
            if (entry.getValue().size() > 1) {
                Map<String, Object> group = new LinkedHashMap<>();
                group.put("fingerprint", entry.getKey());
                group.put("req_ids", entry.getValue());
                dupGroups.add(group);
            }
        }
        updateRunResult(runId, "dedup_result", Map.of("duplicate_groups", dupGroups.size(), "groups", dupGroups));
    }

    private void stepTrace(String runId, String sessionId, String projectId) {
        List<RequirementL123> all = l123Repository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        if (all.size() < 2) {
            return;
        }

        List<Map<String, Object>> pairs = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            for (int j = i + 1; j < all.size(); j++) {
                Map<String, Object> pair = new LinkedHashMap<>();
                pair.put("req_a_id", all.get(i).getReqId());
                pair.put("req_a_text", all.get(i).getText());
                pair.put("req_b_id", all.get(j).getReqId());
                pair.put("req_b_text", all.get(j).getText());
                pairs.add(pair);
            }
        }

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("session_id", sessionId);
        request.put("pairs", pairs);

        try {
            Map<String, Object> response = inferenceServiceClient.batchAnalyzeRelations(request);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> relations = (List<Map<String, Object>>) response.getOrDefault("relations", List.of());
            String snapshotId = UUID.randomUUID().toString();

            for (Map<String, Object> rel : relations) {
                RequirementGraphRelation graphRel = new RequirementGraphRelation();
                graphRel.setSnapshotId(snapshotId);
                graphRel.setRunId(runId);
                graphRel.setProjectId(projectId);
                graphRel.setSessionId(sessionId);
                graphRel.setRelationMode("traceability");
                graphRel.setSourceNodeId((String) rel.getOrDefault("source_node_id", UUID.randomUUID().toString()));
                graphRel.setTargetNodeId((String) rel.getOrDefault("target_node_id", UUID.randomUUID().toString()));
                graphRel.setSourceReqId((String) rel.getOrDefault("source_req_id", ""));
                graphRel.setTargetReqId((String) rel.getOrDefault("target_req_id", ""));
                graphRel.setRelationType((String) rel.getOrDefault("relation_type", "related"));
                graphRel.setSourceKind((String) rel.getOrDefault("source_kind", "l3"));
                graphRel.setWeight(((Number) rel.getOrDefault("weight", 0.5)).doubleValue());
                graphRel.setConfidence(rel.get("confidence") != null ? ((Number) rel.get("confidence")).doubleValue() : null);
                graphRel.setReason((String) rel.getOrDefault("reason", ""));
                graphRel.setModel((String) rel.getOrDefault("model", ""));
                graphRel.setEvidence(jsonSupport.toJson(rel.get("evidence")));
                graphRel.setIsActive(true);
                graphRel.setCreatedAt(Instant.now());
                graphRepository.save(graphRel);
            }

            updateRunResult(runId, "trace_result", Map.of("snapshot_id", snapshotId, "total_relations", relations.size()));
        } catch (Exception e) {
            log.warn("Trace analysis failed: {}", e.getMessage());
        }
    }

    private void stepConflict(String runId, String sessionId) {
        List<RequirementL123> all = l123Repository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        if (all.size() < 2) {
            return;
        }

        String batchId = UUID.randomUUID().toString();
        List<Map<String, Object>> conflicts = new ArrayList<>();

        for (int i = 0; i < all.size(); i++) {
            for (int j = i + 1; j < all.size(); j++) {
                Map<String, Object> request = new LinkedHashMap<>();
                request.put("requirement_a", all.get(i).getText());
                request.put("requirement_b", all.get(j).getText());

                try {
                    Map<String, Object> response = inferenceServiceClient.checkConflict(request);

                    ConflictAnalysis conflict = new ConflictAnalysis();
                    conflict.setBatchId(batchId);
                    conflict.setSessionId(sessionId);
                    conflict.setRequirementA(all.get(i).getText());
                    conflict.setRequirementB(all.get(j).getText());
                    conflict.setIsConflict((Boolean) response.getOrDefault("is_conflict", false));
                    conflict.setResultJson(jsonSupport.toJson(response));
                    conflict.setCreatedAt(Instant.now());
                    conflictRepository.save(conflict);

                    if (Boolean.TRUE.equals(conflict.getIsConflict())) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", conflict.getId());
                        item.put("req_a_id", all.get(i).getReqId());
                        item.put("req_b_id", all.get(j).getReqId());
                        conflicts.add(item);
                    }
                } catch (Exception e) {
                    log.warn("Conflict check failed for pair: {}", e.getMessage());
                }
            }
        }

        updateRunResult(runId, "conflict_result", Map.of("batch_id", batchId, "total_conflicts", conflicts.size(), "conflicts", conflicts));
    }

    private void stepImport(String runId, String sessionId, String projectId, String mappingMode) {
        String normalizedMode = mappingMode == null || mappingMode.isBlank()
                ? "tree"
                : mappingMode.trim().toLowerCase();

        Map<String, Object> response = requirementServiceClient.importRequirements(
                projectId,
                new RequirementImportRequest(sessionId, normalizedMode)
        );

        if (response.containsKey("error")) {
            throw new ErrorResponse(String.valueOf(response.getOrDefault("detail", "需求管理服务导入失败")), 503);
        }

        updateRunResult(runId, "import_result", response);
    }

    private void updateRunResult(String runId, String field, Object value) {
        runRepository.findById(runId).ifPresent(run -> {
            switch (field) {
                case "high_level_requirements" -> run.setHighLevelRequirements(jsonSupport.toJson(value));
                case "low_level_requirements" -> run.setLowLevelRequirements(jsonSupport.toJson(value));
                case "trace_result" -> run.setTraceResult(jsonSupport.toJson(value));
                case "conflict_result" -> run.setConflictResult(jsonSupport.toJson(value));
                case "classification_result" -> run.setClassificationResult(jsonSupport.toJson(value));
                case "dedup_result" -> run.setMetaJson(jsonSupport.toJson(value));
                case "import_result" -> run.setMetaJson(jsonSupport.toJson(Map.of(
                        "dedup_result", jsonSupport.toMap(run.getMetaJson()),
                        "import_result", value
                )));
            }
            runRepository.save(run);
        });
    }
}
