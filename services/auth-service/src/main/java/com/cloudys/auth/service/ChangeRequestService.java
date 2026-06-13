package com.cloudys.auth.service;

import com.cloudys.auth.entity.ChangeRequest;
import com.cloudys.auth.repository.ChangeRequestRepository;
import com.cloudys.common.core.exception.ErrorResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class ChangeRequestService {

    private final ChangeRequestRepository crRepo;

    public ChangeRequestService(ChangeRequestRepository crRepo) {
        this.crRepo = crRepo;
    }

    @Transactional
    public Map<String, Object> create(String requirementId, Long baselineId, String reason,
                                       String changeSummary, String requestedBy) {
        if (reason == null || reason.isBlank() || changeSummary == null || changeSummary.isBlank()) {
            throw new ErrorResponse("变更原因和变更摘要不能为空", 400);
        }

        ChangeRequest cr = new ChangeRequest();
        cr.setRequirementId(requirementId);
        cr.setBaselineId(baselineId);
        cr.setReason(reason.trim());
        cr.setChangeSummary(changeSummary.trim());
        cr.setRequestedBy(requestedBy);
        cr = crRepo.save(cr);

        return toMap(cr);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listByRequirement(String requirementId) {
        return crRepo.findByRequirementIdOrderByCreatedAtDesc(requirementId).stream()
                .map(this::toMap).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getById(Long id) {
        ChangeRequest cr = crRepo.findById(id)
                .orElseThrow(() -> new ErrorResponse("变更请求不存在", 404));
        return toMap(cr);
    }

    @Transactional
    public Map<String, Object> approve(Long id, String reviewComment, String reviewedBy) {
        ChangeRequest cr = crRepo.findById(id)
                .orElseThrow(() -> new ErrorResponse("变更请求不存在", 404));
        if (!"PENDING".equals(cr.getStatus())) {
            throw new ErrorResponse("该变更请求已处理", 400);
        }

        cr.setStatus("APPROVED");
        cr.setReviewedBy(reviewedBy);
        cr.setReviewComment(reviewComment);
        cr.setResolvedAt(Instant.now());
        return toMap(crRepo.save(cr));
    }

    @Transactional
    public Map<String, Object> reject(Long id, String reviewComment, String reviewedBy) {
        ChangeRequest cr = crRepo.findById(id)
                .orElseThrow(() -> new ErrorResponse("变更请求不存在", 404));
        if (!"PENDING".equals(cr.getStatus())) {
            throw new ErrorResponse("该变更请求已处理", 400);
        }

        cr.setStatus("REJECTED");
        cr.setReviewedBy(reviewedBy);
        cr.setReviewComment(reviewComment);
        cr.setResolvedAt(Instant.now());
        return toMap(crRepo.save(cr));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPending() {
        return crRepo.findByStatus("PENDING").stream().map(this::toMap).toList();
    }

    private Map<String, Object> toMap(ChangeRequest cr) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", cr.getId());
        map.put("requirement_id", cr.getRequirementId());
        map.put("baseline_id", cr.getBaselineId());
        map.put("requested_by", cr.getRequestedBy());
        map.put("reason", cr.getReason());
        map.put("change_summary", cr.getChangeSummary());
        map.put("status", cr.getStatus());
        map.put("reviewed_by", cr.getReviewedBy());
        map.put("review_comment", cr.getReviewComment());
        map.put("created_at", cr.getCreatedAt());
        map.put("resolved_at", cr.getResolvedAt());
        return map;
    }
}
