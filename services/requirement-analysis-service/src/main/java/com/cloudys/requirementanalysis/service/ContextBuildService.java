package com.cloudys.requirementanalysis.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cloudys.common.core.exception.ErrorResponse;
import com.cloudys.requirementanalysis.dto.AddBundleItemRequest;
import com.cloudys.requirementanalysis.dto.CreateBundleRequest;
import com.cloudys.requirementanalysis.dto.CreateContextRunRequest;
import com.cloudys.requirementanalysis.dto.CreateSpanLinkRequest;
import com.cloudys.requirementanalysis.dto.CreateSpanRequest;
import com.cloudys.requirementanalysis.entity.Bundle;
import com.cloudys.requirementanalysis.entity.BundleItem;
import com.cloudys.requirementanalysis.entity.ContextRun;
import com.cloudys.requirementanalysis.entity.Span;
import com.cloudys.requirementanalysis.entity.SpanLink;
import com.cloudys.requirementanalysis.repository.BundleItemRepository;
import com.cloudys.requirementanalysis.repository.BundleRepository;
import com.cloudys.requirementanalysis.repository.ContextRunRepository;
import com.cloudys.requirementanalysis.repository.SpanLinkRepository;
import com.cloudys.requirementanalysis.repository.SpanRepository;

@Service
public class ContextBuildService {

    private final SpanRepository spanRepository;
    private final ContextRunRepository contextRunRepository;
    private final SpanLinkRepository spanLinkRepository;
    private final BundleRepository bundleRepository;
    private final BundleItemRepository bundleItemRepository;
    private final JsonSupport jsonSupport;

    public ContextBuildService(SpanRepository spanRepository,
                               ContextRunRepository contextRunRepository,
                               SpanLinkRepository spanLinkRepository,
                               BundleRepository bundleRepository,
                               BundleItemRepository bundleItemRepository,
                               JsonSupport jsonSupport) {
        this.spanRepository = spanRepository;
        this.contextRunRepository = contextRunRepository;
        this.spanLinkRepository = spanLinkRepository;
        this.bundleRepository = bundleRepository;
        this.bundleItemRepository = bundleItemRepository;
        this.jsonSupport = jsonSupport;
    }

    @Transactional
    public Map<String, Object> createSpan(CreateSpanRequest request) {
        Span span = new Span();
        span.setSpanId(UUID.randomUUID().toString());
        span.setSessionId(request.sessionId());
        span.setStartMs(request.startMs());
        span.setEndMs(request.endMs());
        span.setSpeaker(request.speaker());
        span.setText(request.text());
        span.setAsrConfidence(request.asrConfidence());
        span.setMetaJson(jsonSupport.toJson(request.meta()));
        span.setCreatedAt(Instant.now());
        span.setUpdatedAt(Instant.now());
        spanRepository.save(span);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("span_id", span.getSpanId());
        result.put("created_at", span.getCreatedAt());
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listSpans(String sessionId) {
        List<Span> spans = spanRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("session_id", sessionId);
        result.put("spans", spans.stream().map(this::toSpanMap).toList());
        return result;
    }

    @Transactional
    public Map<String, Object> createContextRun(CreateContextRunRequest request) {
        ContextRun run = new ContextRun();
        run.setContextRunId(UUID.randomUUID().toString());
        run.setSessionId(request.sessionId());
        run.setOptionsSnapshot(jsonSupport.toJson(request.optionsSnapshot()));
        run.setStatus("RUNNING");
        run.setCreatedAt(Instant.now());
        contextRunRepository.save(run);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("context_run_id", run.getContextRunId());
        result.put("created_at", run.getCreatedAt());
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listContextRuns(String sessionId) {
        List<ContextRun> runs = contextRunRepository.findBySessionIdOrderByCreatedAtDesc(sessionId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("session_id", sessionId);
        result.put("runs", runs.stream().map(this::toContextRunMap).toList());
        return result;
    }

    @Transactional
    public Map<String, Object> createSpanLink(CreateSpanLinkRequest request) {
        spanRepository.findById(request.sourceSpanId())
                .orElseThrow(() -> new ErrorResponse("source_span_id 不存在: " + request.sourceSpanId(), 404));
        spanRepository.findById(request.targetSpanId())
                .orElseThrow(() -> new ErrorResponse("target_span_id 不存在: " + request.targetSpanId(), 404));
        contextRunRepository.findById(request.contextRunId())
                .orElseThrow(() -> new ErrorResponse("context_run_id 不存在: " + request.contextRunId(), 404));

        SpanLink link = new SpanLink();
        link.setContextRunId(request.contextRunId());
        link.setSourceSpanId(request.sourceSpanId());
        link.setTargetSpanId(request.targetSpanId());
        link.setRelationType(request.relationType());
        link.setStrength(request.strength());
        link.setNote(request.note());
        link.setCreatedAt(Instant.now());
        spanLinkRepository.save(link);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", link.getId());
        result.put("created_at", link.getCreatedAt());
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listSpanLinks(String contextRunId) {
        List<SpanLink> links = spanLinkRepository.findByContextRunId(contextRunId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("context_run_id", contextRunId);
        result.put("links", links.stream().map(this::toSpanLinkMap).toList());
        return result;
    }

    @Transactional
    public Map<String, Object> createBundle(CreateBundleRequest request) {
        contextRunRepository.findById(request.contextRunId())
                .orElseThrow(() -> new ErrorResponse("context_run_id 不存在: " + request.contextRunId(), 404));

        Bundle bundle = new Bundle();
        bundle.setBundleId(UUID.randomUUID().toString());
        bundle.setContextRunId(request.contextRunId());
        bundle.setSessionId(request.sessionId());
        bundle.setOrderIndex(request.orderIndex() != null ? request.orderIndex() : nextBundleOrder(request.contextRunId()));
        bundle.setContextSummary(request.contextSummary());
        bundle.setMeta(jsonSupport.toJson(request.meta()));
        bundle.setCreatedAt(Instant.now());
        bundleRepository.save(bundle);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bundle_id", bundle.getBundleId());
        result.put("created_at", bundle.getCreatedAt());
        return result;
    }

    @Transactional
    public Map<String, Object> addBundleItem(String bundleId, AddBundleItemRequest request) {
        Bundle bundle = bundleRepository.findById(bundleId)
                .orElseThrow(() -> new ErrorResponse("bundle_id 不存在: " + bundleId, 404));
        spanRepository.findById(request.spanId())
                .orElseThrow(() -> new ErrorResponse("span_id 不存在: " + request.spanId(), 404));

        BundleItem item = new BundleItem();
        item.setBundleId(bundle.getBundleId());
        item.setSpanId(request.spanId());
        item.setSpanRef(request.spanRef());
        item.setOrderIndex(request.orderIndex());
        bundleItemRepository.save(item);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.getId());
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listBundles(String contextRunId) {
        List<Bundle> bundles = bundleRepository.findByContextRunIdOrderByOrderIndexAsc(contextRunId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("context_run_id", contextRunId);
        result.put("bundles", bundles.stream().map(b -> {
            Map<String, Object> bm = toBundleMap(b);
            List<BundleItem> items = bundleItemRepository.findByBundleIdOrderByOrderIndexAsc(b.getBundleId());
            bm.put("items", items.stream().map(this::toBundleItemMap).toList());
            return bm;
        }).toList());
        return result;
    }

    private int nextBundleOrder(String contextRunId) {
        List<Bundle> existing = bundleRepository.findByContextRunIdOrderByOrderIndexAsc(contextRunId);
        return existing.isEmpty() ? 0 : existing.get(existing.size() - 1).getOrderIndex() + 1;
    }

    private Map<String, Object> toSpanMap(Span span) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("span_id", span.getSpanId());
        map.put("session_id", span.getSessionId());
        map.put("start_ms", span.getStartMs());
        map.put("end_ms", span.getEndMs());
        map.put("speaker", span.getSpeaker());
        map.put("text", span.getText());
        map.put("asr_confidence", span.getAsrConfidence());
        map.put("meta", jsonSupport.toMap(span.getMetaJson()));
        map.put("created_at", span.getCreatedAt());
        map.put("updated_at", span.getUpdatedAt());
        return map;
    }

    private Map<String, Object> toContextRunMap(ContextRun run) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("context_run_id", run.getContextRunId());
        map.put("session_id", run.getSessionId());
        map.put("options_snapshot", jsonSupport.toMap(run.getOptionsSnapshot()));
        map.put("status", run.getStatus());
        map.put("stats", jsonSupport.toMap(run.getStats()));
        map.put("created_at", run.getCreatedAt());
        return map;
    }

    private Map<String, Object> toSpanLinkMap(SpanLink link) {
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
    }

    private Map<String, Object> toBundleMap(Bundle bundle) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("bundle_id", bundle.getBundleId());
        map.put("context_run_id", bundle.getContextRunId());
        map.put("session_id", bundle.getSessionId());
        map.put("order_index", bundle.getOrderIndex());
        map.put("status", bundle.getStatus());
        map.put("context_summary", bundle.getContextSummary());
        map.put("meta", jsonSupport.toMap(bundle.getMeta()));
        map.put("created_at", bundle.getCreatedAt());
        return map;
    }

    private Map<String, Object> toBundleItemMap(BundleItem item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("bundle_id", item.getBundleId());
        map.put("span_id", item.getSpanId());
        map.put("span_ref", item.getSpanRef());
        map.put("order_index", item.getOrderIndex());
        return map;
    }
}
