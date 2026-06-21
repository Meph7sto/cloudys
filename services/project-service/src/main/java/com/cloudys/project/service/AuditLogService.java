package com.cloudys.project.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
                .findDashboardRowsByProjectId(projectId, safeLimit)
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

    private Map<String, Object> toMap(Map<String, Object> row) {
        Map<String, Object> payload = JsonSupport.toMap(stringValue(row.get("detail")));
        String action = stringValue(row.get("action"));
        String description = String.valueOf(payload.getOrDefault("description",
                payload.getOrDefault("detail", payload.getOrDefault("summary", action))));

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("log_id", row.get("log_id"));
        map.put("project_id", row.get("project_id"));
        map.put("product_id", row.get("product_id"));
        map.put("actor", row.get("actor"));
        map.put("action", action);
        map.put("target_type", row.get("target_type"));
        map.put("target_id", row.get("target_id"));
        map.put("detail", description);
        map.put("description", description);
        map.put("summary", description);
        map.put("payload", payload);
        map.put("created_at", row.get("created_at"));
        return map;
    }

    private static String stringValue(Object value) {
        return value != null ? String.valueOf(value) : null;
    }
}
