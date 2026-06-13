package com.cloudys.auth.service;

import com.cloudys.auth.entity.ReviewAssignment;
import com.cloudys.auth.repository.ReviewAssignmentRepository;
import com.cloudys.common.core.exception.ErrorResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class ReviewService {

    private final ReviewAssignmentRepository reviewRepo;

    public ReviewService(ReviewAssignmentRepository reviewRepo) {
        this.reviewRepo = reviewRepo;
    }

    @Transactional
    public Map<String, Object> create(String requirementId, String reviewerId, Short seq,
                                       String comment, String initiatedBy) {
        ReviewAssignment review = new ReviewAssignment();
        review.setRequirementId(requirementId);
        review.setReviewerId(reviewerId);
        review.setSeq(seq != null ? seq : (short) 1);
        review.setComment(comment);
        review.setInitiatedBy(initiatedBy);
        review = reviewRepo.save(review);

        return toMap(review);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listByRequirement(String requirementId) {
        return reviewRepo.findByRequirementId(requirementId).stream().map(this::toMap).toList();
    }

    @Transactional
    public Map<String, Object> approve(Long id, String reviewerId, String comment) {
        ReviewAssignment review = reviewRepo.findByIdAndReviewerIdAndStatus(id, reviewerId, "PENDING")
                .orElseThrow(() -> new ErrorResponse("评审记录不存在或已处理", 404));

        review.setStatus("APPROVED");
        review.setComment(comment);
        review.setDecidedAt(Instant.now());
        return toMap(reviewRepo.save(review));
    }

    @Transactional
    public Map<String, Object> reject(Long id, String reviewerId, String comment) {
        ReviewAssignment review = reviewRepo.findByIdAndReviewerIdAndStatus(id, reviewerId, "PENDING")
                .orElseThrow(() -> new ErrorResponse("评审记录不存在或已处理", 404));

        review.setStatus("REJECTED");
        review.setComment(comment);
        review.setDecidedAt(Instant.now());
        return toMap(reviewRepo.save(review));
    }

    @Transactional
    public Map<String, Object> withdraw(Long id, String initiatedBy) {
        ReviewAssignment review = reviewRepo.findByIdAndInitiatedByAndStatus(id, initiatedBy, "PENDING")
                .orElseThrow(() -> new ErrorResponse("评审记录不存在或已处理", 404));

        review.setStatus("WITHDRAWN");
        review.setDecidedAt(Instant.now());
        return toMap(reviewRepo.save(review));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUserPendingReviews(String userId) {
        return reviewRepo.findByReviewerIdAndStatus(userId, "PENDING").stream()
                .map(this::toMap).toList();
    }

    private Map<String, Object> toMap(ReviewAssignment r) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", r.getId());
        map.put("requirement_id", r.getRequirementId());
        map.put("reviewer_id", r.getReviewerId());
        map.put("initiated_by", r.getInitiatedBy());
        map.put("seq", r.getSeq());
        map.put("status", r.getStatus());
        map.put("comment", r.getComment());
        map.put("decided_at", r.getDecidedAt());
        map.put("created_at", r.getCreatedAt());
        return map;
    }
}
