package com.cloudys.project.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cloudys.project.entity.AuditLog;
import com.cloudys.project.repository.AuditLogRepository;
import com.cloudys.project.util.SecurityUtils;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(String projectId,
                       String productId,
                       String action,
                       String targetType,
                       String targetId,
                       Map<String, Object> detail) {
        AuditLog log = new AuditLog();
        log.setLogId("audit-" + UUID.randomUUID());
        log.setProjectId(projectId);
        log.setProductId(productId);
        log.setActor(SecurityUtils.getCurrentUserId());
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(JsonSupport.toJson(detail != null ? detail : Map.of()));
        log.setCreatedAt(Instant.now());
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listProjectAudits(String projectId, int limit) {
        int safeLimit = limit > 0 ? Math.min(limit, 200) : 100;
        List<Map<String, Object>> rows = auditLogRepository
                .findByProjectIdOrderByCreatedAtDesc(projectId, PageRequest.of(0, safeLimit))
                .stream()
                .map(this::toMap)
                .toList();

        return Map.of(
                "logs", rows,
                "audits", rows,
                "events", rows
        );
    }

    private Map<String, Object> toMap(AuditLog log) {
        Map<String, Object> payload = JsonSupport.toMap(log.getDetail());
        String description = String.valueOf(payload.getOrDefault("description",
                payload.getOrDefault("detail", payload.getOrDefault("summary", log.getAction()))));

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("log_id", log.getLogId());
        map.put("project_id", log.getProjectId());
        map.put("product_id", log.getProductId());
        map.put("actor", log.getActor());
        map.put("action", log.getAction());
        map.put("target_type", log.getTargetType());
        map.put("target_id", log.getTargetId());
        map.put("detail", description);
        map.put("description", description);
        map.put("summary", description);
        map.put("payload", payload);
        map.put("created_at", log.getCreatedAt());
        return map;
    }
}
