package com.cloudys.requirementanalysis.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cloudys.common.core.exception.ErrorResponse;
import com.cloudys.requirementanalysis.entity.RequirementL123;
import com.cloudys.requirementanalysis.repository.RequirementL123Repository;

@Service
public class DedupService {

    private final RequirementL123Repository requirementL123Repository;

    public DedupService(RequirementL123Repository requirementL123Repository) {
        this.requirementL123Repository = requirementL123Repository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detectDuplicates(String sessionId, List<String> requirementTexts) {
        List<Map<String, Object>> duplicates = new ArrayList<>();
        List<Map<String, Object>> unique = new ArrayList<>();

        for (int i = 0; i < requirementTexts.size(); i++) {
            String text = requirementTexts.get(i);
            String fingerprint = computeFingerprint(text);

            List<RequirementL123> existing = requirementL123Repository.findBySessionIdOrderByCreatedAtAsc(sessionId);
            boolean isDuplicate = existing.stream().anyMatch(r -> r.getFingerprint().equals(fingerprint));

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("index", i);
            entry.put("text", text);
            entry.put("fingerprint", fingerprint);

            if (isDuplicate) {
                entry.put("duplicate_of", existing.stream()
                        .filter(r -> r.getFingerprint().equals(fingerprint))
                        .findFirst()
                        .map(RequirementL123::getReqId)
                        .orElse(null));
                duplicates.add(entry);
            } else {
                unique.add(entry);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("session_id", sessionId);
        result.put("total", requirementTexts.size());
        result.put("unique_count", unique.size());
        result.put("duplicate_count", duplicates.size());
        result.put("duplicates", duplicates);
        result.put("unique", unique);
        result.put("score", requirementTexts.isEmpty() ? 100.0 :
                (double) unique.size() / requirementTexts.size() * 100.0);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDedupResults(String sessionId) {
        List<RequirementL123> requirements = requirementL123Repository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        if (requirements.isEmpty()) {
            throw new ErrorResponse("session 不存在去重结果: " + sessionId, 404);
        }

        Map<String, List<RequirementL123>> byFingerprint = new java.util.LinkedHashMap<>();
        for (RequirementL123 req : requirements) {
            byFingerprint.computeIfAbsent(req.getFingerprint(), k -> new ArrayList<>()).add(req);
        }

        List<Map<String, Object>> groups = new ArrayList<>();
        for (Map.Entry<String, List<RequirementL123>> entry : byFingerprint.entrySet()) {
            if (entry.getValue().size() > 1) {
                Map<String, Object> group = new LinkedHashMap<>();
                group.put("fingerprint", entry.getKey());
                group.put("count", entry.getValue().size());
                group.put("req_ids", entry.getValue().stream().map(RequirementL123::getReqId).toList());
                groups.add(group);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("session_id", sessionId);
        result.put("total_requirements", requirements.size());
        result.put("duplicate_groups", groups.size());
        result.put("groups", groups);
        return result;
    }

    private String computeFingerprint(String text) {
        try {
            String normalized = text.toLowerCase().trim().replaceAll("[^a-z0-9\\u4e00-\\u9fff\\s]", "");
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new ErrorResponse("指纹计算失败", 500);
        }
    }
}
