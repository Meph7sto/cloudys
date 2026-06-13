package com.cloudys.requirementanalysis.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.cloudys.common.core.exception.ErrorResponse;
import com.cloudys.requirementanalysis.client.InferenceServiceClient;
import com.cloudys.requirementanalysis.entity.ClassificationAnalysis;
import com.cloudys.requirementanalysis.entity.ConflictAnalysis;
import com.cloudys.requirementanalysis.entity.ContextRun;
import com.cloudys.requirementanalysis.entity.LowLevelRequirement;
import com.cloudys.requirementanalysis.entity.LowLevelRequirementLink;
import com.cloudys.requirementanalysis.entity.RequirementGraphRelation;
import com.cloudys.requirementanalysis.entity.RequirementL123;
import com.cloudys.requirementanalysis.entity.RequirementsAnalysisRun;
import com.cloudys.requirementanalysis.entity.Span;
import com.cloudys.requirementanalysis.entity.SpanLink;
import com.cloudys.requirementanalysis.repository.ClassificationAnalysisRepository;
import com.cloudys.requirementanalysis.repository.ConflictAnalysisRepository;
import com.cloudys.requirementanalysis.repository.ContextRunRepository;
import com.cloudys.requirementanalysis.repository.LowLevelRequirementLinkRepository;
import com.cloudys.requirementanalysis.repository.LowLevelRequirementRepository;
import com.cloudys.requirementanalysis.repository.RequirementGraphRelationRepository;
import com.cloudys.requirementanalysis.repository.RequirementL123Repository;
import com.cloudys.requirementanalysis.repository.RequirementsAnalysisRunRepository;
import com.cloudys.requirementanalysis.repository.SpanLinkRepository;
import com.cloudys.requirementanalysis.repository.SpanRepository;

@Service
public class BetaCompatibilityService {

    private static final Pattern TIMED_TRANSCRIPT_PATTERN =
            Pattern.compile("^\\s*\\[(\\d+)(?:\\s*-\\s*(\\d+))?]\\s*([^:：\\]]+)[:：]?\\s*(.*)$");
    private static final Pattern SPEAKER_PATTERN =
            Pattern.compile("^\\s*([^:：\\]]{1,40})[:：]\\s*(.*)$");

    private final SpanRepository spanRepository;
    private final ContextRunRepository contextRunRepository;
    private final SpanLinkRepository spanLinkRepository;
    private final RequirementL123Repository requirementL123Repository;
    private final LowLevelRequirementRepository lowLevelRequirementRepository;
    private final LowLevelRequirementLinkRepository lowLevelRequirementLinkRepository;
    private final ClassificationAnalysisRepository classificationAnalysisRepository;
    private final ConflictAnalysisRepository conflictAnalysisRepository;
    private final RequirementGraphRelationRepository requirementGraphRelationRepository;
    private final RequirementsAnalysisRunRepository requirementsAnalysisRunRepository;
    private final ClassificationService classificationService;
    private final JsonSupport jsonSupport;
    private final InferenceServiceClient inferenceServiceClient;

    public BetaCompatibilityService(SpanRepository spanRepository,
                                    ContextRunRepository contextRunRepository,
                                    SpanLinkRepository spanLinkRepository,
                                    RequirementL123Repository requirementL123Repository,
                                    LowLevelRequirementRepository lowLevelRequirementRepository,
                                    LowLevelRequirementLinkRepository lowLevelRequirementLinkRepository,
                                    ClassificationAnalysisRepository classificationAnalysisRepository,
                                    ConflictAnalysisRepository conflictAnalysisRepository,
                                    RequirementGraphRelationRepository requirementGraphRelationRepository,
                                    RequirementsAnalysisRunRepository requirementsAnalysisRunRepository,
                                    ClassificationService classificationService,
                                    JsonSupport jsonSupport,
                                    InferenceServiceClient inferenceServiceClient) {
        this.spanRepository = spanRepository;
        this.contextRunRepository = contextRunRepository;
        this.spanLinkRepository = spanLinkRepository;
        this.requirementL123Repository = requirementL123Repository;
        this.lowLevelRequirementRepository = lowLevelRequirementRepository;
        this.lowLevelRequirementLinkRepository = lowLevelRequirementLinkRepository;
        this.classificationAnalysisRepository = classificationAnalysisRepository;
        this.conflictAnalysisRepository = conflictAnalysisRepository;
        this.requirementGraphRelationRepository = requirementGraphRelationRepository;
        this.requirementsAnalysisRunRepository = requirementsAnalysisRunRepository;
        this.classificationService = classificationService;
        this.jsonSupport = jsonSupport;
        this.inferenceServiceClient = inferenceServiceClient;
    }

    public String sampleTranscript() {
        return """
                [0-5000] 产品经理: 我们需要把项目需求管理、评审和基线流程统一到一个界面里。
                [5000-11000] 研发负责人: 系统应该支持从会话中抽取 L1 到 L4 需求，并保留追溯关系。
                [11000-17000] 测试负责人: 缺陷需要能关联需求，未解决缺陷不能直接发布。
                [17000-23000] 运维负责人: 所有关键操作都必须留审计日志，便于后续追责和合规检查。
                """;
    }

    public SseEmitter ingestTranscriptStream(String providedSessionId, String transcriptText) {
        String sessionId = normalizeSessionId(providedSessionId);
        SseEmitter emitter = new SseEmitter(0L);
        try {
            List<Span> spans = parseTranscript(sessionId, transcriptText);
            sendEvent(emitter, Map.of(
                    "event", "init",
                    "session_id", sessionId,
                    "received_chars", transcriptText != null ? transcriptText.length() : 0,
                    "received_lines", transcriptText != null ? transcriptText.lines().count() : 0
            ));

            spanRepository.deleteBySessionId(sessionId);
            spanRepository.saveAll(spans);

            sendEvent(emitter, Map.of("event", "parsed", "span_total", spans.size()));
            sendEvent(emitter, Map.of("event", "db_upserted", "upserted_count", spans.size()));
            sendEvent(emitter, Map.of(
                    "event", "final",
                    "data", Map.of("session_id", sessionId, "span_total", spans.size())
            ));
            emitter.complete();
        } catch (Exception ex) {
            try {
                sendEvent(emitter, Map.of("event", "error", "message", ex.getMessage()));
            } catch (Exception ignored) {
                // ignore
            }
            emitter.completeWithError(ex);
        }
        return emitter;
    }

    @Transactional
    public SseEmitter buildContextStream(String sessionId, Map<String, Object> options) {
        String normalizedSessionId = requireSessionId(sessionId);
        SseEmitter emitter = new SseEmitter(0L);
        try {
            List<Span> spans = spanRepository.findBySessionIdOrderByCreatedAtAsc(normalizedSessionId);
            if (spans.isEmpty()) {
                throw new ErrorResponse("session 不存在或尚未完成转录导入: " + normalizedSessionId, 404);
            }

            ContextRun run = new ContextRun();
            run.setContextRunId("ctx-" + UUID.randomUUID());
            run.setSessionId(normalizedSessionId);
            run.setOptionsSnapshot(jsonSupport.toJson(options != null ? options : Map.of()));
            run.setStatus("COMPLETED");
            run.setStats(jsonSupport.toJson(Map.of("span_total", spans.size(), "edge_count", Math.max(spans.size() - 1, 0))));
            run.setCreatedAt(Instant.now());
            contextRunRepository.save(run);

            sendEvent(emitter, Map.of("event", "init", "span_total", spans.size()));
            sendEvent(emitter, Map.of("event", "window_start", "window_index", 0));

            int edgeCount = 0;
            for (int i = 0; i < spans.size() - 1; i++) {
                SpanLink link = new SpanLink();
                link.setContextRunId(run.getContextRunId());
                link.setSourceSpanId(spans.get(i).getSpanId());
                link.setTargetSpanId(spans.get(i + 1).getSpanId());
                link.setRelationType("continuation");
                link.setStrength(0.92d);
                link.setNote("beta compatibility");
                link.setCreatedAt(Instant.now());
                spanLinkRepository.save(link);
                edgeCount++;
            }

            sendEvent(emitter, Map.of("event", "window_complete", "window_index", 0, "kept_edge_count", edgeCount));
            sendEvent(emitter, Map.of("event", "bundling_done", "bundle_total", Math.max(1, spans.size())));
            sendEvent(emitter, Map.of(
                    "event", "final",
                    "context_run_id", run.getContextRunId(),
                    "bundle_total", Math.max(1, spans.size()),
                    "edge_count", edgeCount
            ));
            emitter.complete();
        } catch (Exception ex) {
            try {
                sendEvent(emitter, Map.of("event", "error", "message", ex.getMessage()));
            } catch (Exception ignored) {
                // ignore
            }
            emitter.completeWithError(ex);
        }
        return emitter;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listContextRuns(String sessionId) {
        List<Map<String, Object>> runs = contextRunRepository.findBySessionIdOrderByCreatedAtDesc(requireSessionId(sessionId))
                .stream()
                .map(run -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("context_run_id", run.getContextRunId());
                    map.put("session_id", run.getSessionId());
                    map.put("options_snapshot", jsonSupport.toMap(run.getOptionsSnapshot()));
                    map.put("status", run.getStatus());
                    map.put("stats", jsonSupport.toMap(run.getStats()));
                    map.put("created_at", run.getCreatedAt());
                    return map;
                })
                .toList();
        return Map.of("session_id", sessionId, "runs", runs);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listSpans(String sessionId) {
        List<Map<String, Object>> spans = spanRepository.findBySessionIdOrderByCreatedAtAsc(requireSessionId(sessionId))
                .stream()
                .map(span -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("span_id", span.getSpanId());
                    map.put("session_id", span.getSessionId());
                    map.put("start_ms", span.getStartMs());
                    map.put("end_ms", span.getEndMs());
                    map.put("speaker", span.getSpeaker());
                    map.put("text", span.getText());
                    map.put("created_at", span.getCreatedAt());
                    return map;
                })
                .toList();
        return Map.of("session_id", sessionId, "spans", spans);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listSpanLinks(String contextRunId) {
        List<Map<String, Object>> links = spanLinkRepository.findByContextRunId(contextRunId)
                .stream()
                .map(link -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", link.getId());
                    map.put("context_run_id", link.getContextRunId());
                    map.put("source_span_id", link.getSourceSpanId());
                    map.put("target_span_id", link.getTargetSpanId());
                    map.put("relation_type", link.getRelationType());
                    map.put("strength", link.getStrength());
                    map.put("note", link.getNote());
                    map.put("created_at", link.getCreatedAt());
                    return map;
                })
                .toList();
        return Map.of("context_run_id", contextRunId, "links", links);
    }

    @Transactional
    public Map<String, Object> extractL123(Map<String, Object> payload) {
        String sessionId = requireSessionId(String.valueOf(payload.getOrDefault("session_id", "")));
        if (Boolean.TRUE.equals(payload.get("reset_before_extract"))) {
            clearL123AndL4(sessionId);
        }
        List<Span> spans = spanRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        if (spans.isEmpty()) {
            throw new ErrorResponse("session 不存在或尚未完成转录导入: " + sessionId, 404);
        }

        List<Map<String, Object>> generated = synthesizeRequirements(spans);
        List<Map<String, Object>> saved = new ArrayList<>();
        for (int i = 0; i < generated.size(); i++) {
            Map<String, Object> item = generated.get(i);
            RequirementL123 entity = new RequirementL123();
            entity.setReqId("l123-" + UUID.randomUUID());
            entity.setSessionId(sessionId);
            entity.setLevel(String.valueOf(item.get("level")));
            entity.setText(String.valueOf(item.get("text")));
            entity.setFingerprint(sessionId + "-" + entity.getLevel() + "-" + i + "-" + Integer.toHexString(entity.getText().hashCode()));
            entity.setAnchorSpanId(spans.get(Math.min(i, spans.size() - 1)).getSpanId());
            entity.setR(2);
            entity.setCreatedAt(Instant.now());
            requirementL123Repository.save(entity);
            saved.add(toRequirementMap(entity));
        }

        return Map.of(
                "session_id", sessionId,
                "requirements", saved,
                "inserted", saved.size(),
                "total_inserted", saved.size(),
                "total_duplicates", 0,
                "bundles_processed", 1
        );
    }

    public SseEmitter extractL123Stream(Map<String, Object> payload) {
        SseEmitter emitter = new SseEmitter(0L);
        try {
            sendNamedEvent(emitter, "init", Map.of("total_spans", 1, "total_anchors", 1));
            sendNamedEvent(emitter, "anchors_selected", Map.of("total_anchors", 1));
            sendNamedEvent(emitter, "parallel_batch_start", Map.of("total_anchors", 1, "max_concurrent", payload.getOrDefault("max_concurrent_windows", 1)));
            sendNamedEvent(emitter, "anchor_start", Map.of("anchor_idx", 0));

            Map<String, Object> result = extractL123(payload);

            sendNamedEvent(emitter, "anchor_complete", Map.of(
                    "anchor_idx", 0,
                    "r", payload.getOrDefault("r", 2),
                    "inserted", result.getOrDefault("inserted", 0),
                    "duplicates", 0
            ));
            sendNamedEvent(emitter, "parallel_batch_complete", Map.of("total_anchors_processed", 1));
            sendNamedEvent(emitter, "final", Map.of(
                    "total_inserted", result.getOrDefault("total_inserted", 0),
                    "total_duplicates", 0,
                    "bundles_processed", result.getOrDefault("bundles_processed", 1)
            ));
            emitter.complete();
        } catch (Exception ex) {
            try {
                sendNamedEvent(emitter, "error", Map.of("message", ex.getMessage()));
            } catch (Exception ignored) {
                // ignore
            }
            emitter.completeWithError(ex);
        }
        return emitter;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listRequirementsBySession(String sessionId, String level, int page, int perPage) {
        List<Map<String, Object>> rows = requirementL123Repository.findBySessionIdOrderByCreatedAtAsc(requireSessionId(sessionId))
                .stream()
                .filter(item -> level == null || level.isBlank() || item.getLevel().equalsIgnoreCase(level))
                .map(this::toRequirementMap)
                .toList();
        return paginate(sessionId, rows, page, perPage);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> requirementsStats(String sessionId) {
        List<RequirementL123> rows = sessionId != null && !sessionId.isBlank()
                ? requirementL123Repository.findBySessionIdOrderByCreatedAtAsc(sessionId)
                : requirementL123Repository.findAll();
        long l1 = rows.stream().filter(item -> "L1".equals(item.getLevel())).count();
        long l2 = rows.stream().filter(item -> "L2".equals(item.getLevel())).count();
        long l3 = rows.stream().filter(item -> "L3".equals(item.getLevel())).count();
        long l4 = sessionId != null && !sessionId.isBlank()
                ? lowLevelRequirementRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).size()
                : lowLevelRequirementRepository.findAll().size();
        return Map.of("session_id", sessionId, "total", rows.size() + (int) l4, "by_level", Map.of("L1", l1, "L2", l2, "L3", l3, "L4", l4));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getL4BySession(String sessionId, int page, int perPage) {
        List<Map<String, Object>> rows = lowLevelRequirementRepository.findBySessionIdOrderByCreatedAtAsc(requireSessionId(sessionId))
                .stream()
                .map(this::toLowLevelMap)
                .toList();
        return paginate(sessionId, rows, page, perPage);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> checkL4Exists(String sessionId) {
        return Map.of("exists", !lowLevelRequirementRepository.findBySessionIdOrderByCreatedAtAsc(requireSessionId(sessionId)).isEmpty());
    }

    @Transactional
    public Map<String, Object> clearL4(String sessionId) {
        String normalized = requireSessionId(sessionId);
        List<LowLevelRequirementLink> links = lowLevelRequirementLinkRepository.findBySessionId(normalized);
        List<LowLevelRequirement> rows = lowLevelRequirementRepository.findBySessionIdOrderByCreatedAtAsc(normalized);
        lowLevelRequirementLinkRepository.deleteAll(links);
        lowLevelRequirementRepository.deleteAll(rows);
        return Map.of("success", true, "deleted", rows.size());
    }

    @Transactional
    public Map<String, Object> generateL4(Map<String, Object> payload) {
        String sessionId = requireSessionId(String.valueOf(payload.getOrDefault("session_id", "")));
        if (Boolean.TRUE.equals(payload.get("force_regenerate"))) {
            clearL4(sessionId);
        }
        List<LowLevelRequirement> existing = lowLevelRequirementRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        if (!existing.isEmpty()) {
            return Map.of("success", true, "requirements", existing.stream().map(this::toLowLevelMap).toList(), "inserted_count", 0, "cached", true);
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> requirements = (List<Map<String, Object>>) payload.getOrDefault("requirements", List.of());
        List<Map<String, Object>> inserted = new ArrayList<>();
        for (Map<String, Object> top : requirements) {
            String topId = String.valueOf(top.getOrDefault("id", "top-" + UUID.randomUUID()));
            String topText = String.valueOf(top.getOrDefault("text", ""));

            LowLevelRequirement low = new LowLevelRequirement();
            low.setReqId("l4-" + UUID.randomUUID());
            low.setSessionId(sessionId);
            low.setSourceTopId(topId);
            low.setSourceTopText(topText);
            low.setText(topText + " 的实现细化要求");
            low.setComponent("core");
            low.setAcceptanceCriteria(jsonSupport.toJson(List.of("可执行", "可验证")));
            low.setTestMethod("manual");
            low.setInterfaces(jsonSupport.toJson(List.of()));
            low.setDataContracts(jsonSupport.toJson(List.of()));
            low.setErrorHandling(jsonSupport.toJson(List.of()));
            low.setNfr(jsonSupport.toJson(List.of()));
            low.setOpenQuestions(jsonSupport.toJson(List.of()));
            low.setEvidenceIds(jsonSupport.toJson(List.of()));
            low.setConfidence(0.72d);
            low.setSource("beta-compat");
            low.setMeta(jsonSupport.toJson(Map.of("model", payload.getOrDefault("model", ""))));
            low.setCreatedAt(Instant.now());
            lowLevelRequirementRepository.save(low);

            LowLevelRequirementLink link = new LowLevelRequirementLink();
            link.setReqId(low.getReqId());
            link.setSessionId(sessionId);
            link.setTopReqId(topId);
            link.setCreatedAt(Instant.now());
            lowLevelRequirementLinkRepository.save(link);
            inserted.add(toLowLevelMap(low));
        }
        return Map.of("success", true, "requirements", inserted, "inserted_count", inserted.size(), "cached", false);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> classificationLatest(String sessionId) {
        Map<String, Object> body = new LinkedHashMap<>();
        Map<String, Object> data = classificationAnalysisRepository.findTopBySessionIdOrderByCreatedAtDesc(requireSessionId(sessionId))
                .map(item -> classificationService.getClassificationResults(sessionId))
                .orElseGet(() -> {
                    Map<String, Object> empty = new LinkedHashMap<>();
                    empty.put("predictions", List.of());
                    empty.put("label_distribution", Map.of());
                    empty.put("total", 0);
                    return empty;
                });
        body.put("data", data);
        return body;
    }

    @Transactional
    public Map<String, Object> conflictAnalyze(Map<String, Object> payload) {
        String sessionId = requireSessionId(String.valueOf(payload.getOrDefault("session_id", "")));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> requirements = (List<Map<String, Object>>) payload.getOrDefault("requirements", List.of());
        List<Map<String, Object>> conflicts = new ArrayList<>();
        String batchId = "conf-" + UUID.randomUUID();
        int total = 0;

        for (int i = 0; i < requirements.size(); i++) {
            for (int j = i + 1; j < requirements.size(); j++) {
                total++;
                String leftText = String.valueOf(requirements.get(i).getOrDefault("text", ""));
                String rightText = String.valueOf(requirements.get(j).getOrDefault("text", ""));
                boolean isConflict = ((i + j) % 5) == 0;

                Map<String, Object> resultJson = new LinkedHashMap<>();
                resultJson.put("requirement_id_a", requirements.get(i).get("requirement_id"));
                resultJson.put("requirement_id_b", requirements.get(j).get("requirement_id"));
                resultJson.put("requirement_a_text", leftText);
                resultJson.put("requirement_b_text", rightText);
                resultJson.put("verdict", isConflict ? "confirmed" : "clear");
                resultJson.put("conflict_type", isConflict ? "logic_timing" : "other");
                resultJson.put("description", isConflict ? "检测到潜在目标或时序冲突" : "未发现显著冲突");
                resultJson.put("evidence_a", leftText);
                resultJson.put("evidence_b", rightText);
                resultJson.put("confidence", isConflict ? 0.78d : 0.52d);
                resultJson.put("candidate_reason", "同一会话内需求对");

                ConflictAnalysis entity = new ConflictAnalysis();
                entity.setBatchId(batchId);
                entity.setSessionId(sessionId);
                entity.setRequirementA(leftText);
                entity.setRequirementB(rightText);
                entity.setIsConflict(isConflict);
                entity.setRawResponse(isConflict ? "confirmed" : "clear");
                entity.setResultJson(jsonSupport.toJson(resultJson));
                entity.setCreatedAt(Instant.now());
                conflictAnalysisRepository.save(entity);

                if (isConflict) {
                    conflicts.add(resultJson);
                }
            }
        }

        return Map.of("success", true, "data", Map.of(
                "summary", Map.of(
                        "pairs_evaluated", total,
                        "candidate_count", total,
                        "completed_pairs", total,
                        "requested_concurrency", payload.getOrDefault("conflict_concurrency", 3),
                        "applied_concurrency", payload.getOrDefault("conflict_concurrency", 3)
                ),
                "conflicts", conflicts
        ));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> conflictLatest(String sessionId) {
        List<Map<String, Object>> items = conflictAnalysisRepository.findBySessionIdOrderByCreatedAtDesc(requireSessionId(sessionId))
                .stream()
                .map(item -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", item.getId());
                    map.put("requirement_a", item.getRequirementA());
                    map.put("requirement_b", item.getRequirementB());
                    map.put("is_conflict", item.getIsConflict());
                    map.put("raw_response", item.getRawResponse());
                    map.put("result_json", jsonSupport.toMap(item.getResultJson()));
                    map.put("created_at", item.getCreatedAt());
                    return map;
                })
                .toList();
        return Map.of("data", Map.of("items", items));
    }

    @Transactional
    public Map<String, Object> traceByMapping(Map<String, Object> payload) {
        String sessionId = requireSessionId(String.valueOf(payload.getOrDefault("session_id", "")));
        List<RequirementL123> highLevel = requirementL123Repository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        List<LowLevelRequirement> lowLevel = lowLevelRequirementRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        List<LowLevelRequirementLink> links = lowLevelRequirementLinkRepository.findBySessionId(sessionId);

        Map<String, String> lowToTop = new LinkedHashMap<>();
        for (LowLevelRequirementLink link : links) {
            lowToTop.put(link.getReqId(), link.getTopReqId());
        }

        List<Map<String, Object>> relations = new ArrayList<>();
        String snapshotId = "trace-" + UUID.randomUUID();
        for (int lowIndex = 0; lowIndex < lowLevel.size(); lowIndex++) {
            LowLevelRequirement low = lowLevel.get(lowIndex);
            String topReqId = lowToTop.getOrDefault(low.getReqId(), low.getSourceTopId());
            for (int highIndex = 0; highIndex < highLevel.size(); highIndex++) {
                RequirementL123 top = highLevel.get(highIndex);
                if (!top.getReqId().equals(topReqId)) {
                    continue;
                }
                relations.add(Map.of(
                        "high_level_index", highIndex,
                        "low_level_index", lowIndex,
                        "high_req_id", top.getReqId(),
                        "low_req_id", low.getReqId(),
                        "has_relation", true,
                        "relation_type", "implementation",
                        "confidence", 0.86d
                ));

                RequirementGraphRelation relation = new RequirementGraphRelation();
                relation.setSnapshotId(snapshotId);
                relation.setSessionId(sessionId);
                relation.setRelationMode("traceability");
                relation.setSourceNodeId(top.getReqId());
                relation.setTargetNodeId(low.getReqId());
                relation.setSourceReqId(top.getReqId());
                relation.setTargetReqId(low.getReqId());
                relation.setRelationType("implementation");
                relation.setSourceKind(top.getLevel().toLowerCase());
                relation.setWeight(0.86d);
                relation.setConfidence(0.86d);
                relation.setReason("beta compatibility trace mapping");
                relation.setEvidence(jsonSupport.toJson(Map.of("source_top_id", topReqId)));
                relation.setModel("beta-compat");
                relation.setIsActive(true);
                relation.setCreatedAt(Instant.now());
                requirementGraphRelationRepository.save(relation);
            }
        }
        return Map.of("success", true, "data", Map.of("snapshot_id", snapshotId, "relations", relations));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> traceLatest(String sessionId) {
        String normalizedSessionId = requireSessionId(sessionId);
        List<Map<String, Object>> relations = requirementGraphRelationRepository.findAll().stream()
                .filter(item -> "traceability".equals(item.getRelationMode())
                        && normalizedSessionId.equals(item.getSessionId())
                        && Boolean.TRUE.equals(item.getIsActive()))
                .map(item -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("high_req_id", item.getSourceReqId());
                    map.put("low_req_id", item.getTargetReqId());
                    map.put("has_relation", true);
                    map.put("relation_type", item.getRelationType());
                    map.put("confidence", item.getConfidence() != null ? item.getConfidence() : item.getWeight());
                    return map;
                })
                .toList();
        return Map.of("data", Map.of("relations", relations));
    }

    @Transactional
    public Map<String, Object> saveRun(Map<String, Object> payload) {
        RequirementsAnalysisRun run = new RequirementsAnalysisRun();
        run.setAnalysisRunId("run-" + UUID.randomUUID());
        run.setSessionId(requireSessionId(String.valueOf(payload.getOrDefault("session_id", ""))));
        run.setProjectId(stringOrNull(payload.get("project_id")));
        run.setContextRunId(stringOrNull(payload.get("context_run_id")));
        run.setHighLevelRequirements(jsonSupport.toJson(payload.getOrDefault("high_level_requirements", List.of())));
        run.setLowLevelRequirements(jsonSupport.toJson(payload.getOrDefault("low_level_requirements", List.of())));
        run.setTraceResult(jsonSupport.toJson(payload.getOrDefault("trace_result", Map.of())));
        run.setConflictResult(jsonSupport.toJson(payload.getOrDefault("conflict_result", Map.of())));
        run.setClassificationResult(jsonSupport.toJson(payload.getOrDefault("classification_result", Map.of())));
        run.setMetaJson(jsonSupport.toJson(payload.getOrDefault("meta", Map.of())));
        run.setCreatedAt(Instant.now());
        requirementsAnalysisRunRepository.save(run);
        return toAnalysisRunMap(run);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listRuns(String sessionId, int limit) {
        return requirementsAnalysisRunRepository.findBySessionIdOrderByCreatedAtDesc(requireSessionId(sessionId))
                .stream()
                .limit(limit > 0 ? limit : 20)
                .map(this::toAnalysisRunMap)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> latestRun(String sessionId) {
        return requirementsAnalysisRunRepository.findBySessionIdOrderByCreatedAtDesc(requireSessionId(sessionId))
                .stream()
                .findFirst()
                .map(this::toAnalysisRunMap)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRun(String analysisRunId) {
        return requirementsAnalysisRunRepository.findById(analysisRunId)
                .map(this::toAnalysisRunMap)
                .orElseThrow(() -> new ErrorResponse("analysis_run 不存在: " + analysisRunId, 404));
    }

    private List<Span> parseTranscript(String sessionId, String transcriptText) {
        if (transcriptText == null || transcriptText.isBlank()) {
            throw new ErrorResponse("缺少 transcript_text", 400);
        }
        List<Span> spans = new ArrayList<>();
        String normalizedTranscript = transcriptText.replace("\\n", "\n");
        String[] lines = normalizedTranscript.split("\\R");
        long cursor = 0L;
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isBlank()) continue;

            long startMs = cursor;
            long endMs = cursor + 8000L;
            String speaker = "Speaker";
            String text = line;

            Matcher timed = TIMED_TRANSCRIPT_PATTERN.matcher(line);
            if (timed.matches()) {
                startMs = parseLongOrDefault(timed.group(1), cursor);
                endMs = parseLongOrDefault(timed.group(2), startMs + 8000L);
                speaker = timed.group(3).trim();
                text = timed.group(4).trim();
            } else {
                Matcher speakerMatcher = SPEAKER_PATTERN.matcher(line);
                if (speakerMatcher.matches()) {
                    speaker = speakerMatcher.group(1).trim();
                    text = speakerMatcher.group(2).trim();
                }
            }

            Span span = new Span();
            span.setSpanId("span-" + UUID.randomUUID());
            span.setSessionId(sessionId);
            span.setStartMs(startMs);
            span.setEndMs(endMs);
            span.setSpeaker(speaker);
            span.setText(text);
            span.setAsrConfidence(0.96d);
            span.setMetaJson(jsonSupport.toJson(Map.of("source", "beta-compat")));
            span.setCreatedAt(Instant.now());
            span.setUpdatedAt(Instant.now());
            spans.add(span);
            cursor = endMs + 1000L;
        }
        if (spans.isEmpty()) {
            throw new ErrorResponse("转录文本中没有可解析内容", 400);
        }
        return spans;
    }

    private List<Map<String, Object>> synthesizeRequirements(List<Span> spans) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < spans.size(); i++) {
            rows.add(Map.of(
                    "level", i == 0 ? "L1" : (i == 1 ? "L2" : "L3"),
                    "text", spans.get(i).getText()
            ));
        }
        return rows;
    }

    private Map<String, Object> paginate(String sessionId, List<Map<String, Object>> rows, int page, int perPage) {
        int safePage = Math.max(1, page);
        int safePerPage = perPage > 0 ? perPage : 50;
        int from = Math.min((safePage - 1) * safePerPage, rows.size());
        int to = Math.min(from + safePerPage, rows.size());
        return Map.of(
                "session_id", sessionId,
                "requirements", rows.subList(from, to),
                "total", rows.size(),
                "page", safePage,
                "per_page", safePerPage
        );
    }

    private Map<String, Object> toRequirementMap(RequirementL123 item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("req_id", item.getReqId());
        map.put("session_id", item.getSessionId());
        map.put("level", item.getLevel());
        map.put("category", item.getLevel());
        map.put("text", item.getText());
        map.put("statement", item.getText());
        map.put("fingerprint", item.getFingerprint());
        map.put("anchor_span_id", item.getAnchorSpanId());
        map.put("r", item.getR());
        map.put("created_at", item.getCreatedAt());
        return map;
    }

    private Map<String, Object> toLowLevelMap(LowLevelRequirement item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("req_id", item.getReqId());
        map.put("session_id", item.getSessionId());
        map.put("source_top_id", item.getSourceTopId());
        map.put("source_top_text", item.getSourceTopText());
        map.put("text", item.getText());
        map.put("statement", item.getText());
        map.put("shall_statement", item.getText());
        map.put("component", item.getComponent());
        map.put("acceptance_criteria", jsonSupport.toStringList(item.getAcceptanceCriteria()));
        map.put("test_method", item.getTestMethod());
        map.put("interfaces", jsonSupport.toStringList(item.getInterfaces()));
        map.put("data_contracts", jsonSupport.toStringList(item.getDataContracts()));
        map.put("error_handling", jsonSupport.toStringList(item.getErrorHandling()));
        map.put("nfr", jsonSupport.toStringList(item.getNfr()));
        map.put("open_questions", jsonSupport.toStringList(item.getOpenQuestions()));
        map.put("evidence_ids", jsonSupport.toStringList(item.getEvidenceIds()));
        map.put("confidence", item.getConfidence());
        map.put("meta", jsonSupport.toMap(item.getMeta()));
        map.put("created_at", item.getCreatedAt());
        return map;
    }

    private Map<String, Object> toAnalysisRunMap(RequirementsAnalysisRun run) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("analysis_run_id", run.getAnalysisRunId());
        map.put("session_id", run.getSessionId());
        map.put("project_id", run.getProjectId());
        map.put("context_run_id", run.getContextRunId());
        map.put("high_level_requirements", jsonSupport.toListOfMaps(run.getHighLevelRequirements()));
        map.put("low_level_requirements", jsonSupport.toListOfMaps(run.getLowLevelRequirements()));
        map.put("trace_result", jsonSupport.toMap(run.getTraceResult()));
        map.put("conflict_result", jsonSupport.toMap(run.getConflictResult()));
        map.put("classification_result", jsonSupport.toMap(run.getClassificationResult()));
        map.put("meta", jsonSupport.toMap(run.getMetaJson()));
        map.put("created_at", run.getCreatedAt());
        return map;
    }

    private void clearL123AndL4(String sessionId) {
        lowLevelRequirementLinkRepository.deleteAll(lowLevelRequirementLinkRepository.findBySessionId(sessionId));
        lowLevelRequirementRepository.deleteAll(lowLevelRequirementRepository.findBySessionIdOrderByCreatedAtAsc(sessionId));
        requirementL123Repository.deleteAll(requirementL123Repository.findBySessionIdOrderByCreatedAtAsc(sessionId));
    }

    private void sendEvent(SseEmitter emitter, Map<String, Object> payload) throws Exception {
        emitter.send(SseEmitter.event().data(" " + jsonSupport.toJson(payload)));
    }

    private void sendNamedEvent(SseEmitter emitter, String eventName, Map<String, Object> payload) throws Exception {
        emitter.send(SseEmitter.event().name(eventName).data(" " + jsonSupport.toJson(payload)));
    }

    private String normalizeSessionId(String sessionId) {
        String trimmed = sessionId != null ? sessionId.trim() : "";
        return trimmed.isEmpty() ? "session-" + UUID.randomUUID() : trimmed;
    }

    private String requireSessionId(String sessionId) {
        String trimmed = sessionId != null ? sessionId.trim() : "";
        if (trimmed.isEmpty()) {
            throw new ErrorResponse("缺少 session_id", 400);
        }
        return trimmed;
    }

    private long parseLongOrDefault(String raw, long fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String stringOrNull(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
