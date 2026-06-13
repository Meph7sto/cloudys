package com.cloudys.auth.controller;

import com.cloudys.auth.dto.request.CreateReviewRequest;
import com.cloudys.auth.service.ReviewService;
import com.cloudys.auth.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/permission")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/requirements/{requirementId}/reviews")
    public ResponseEntity<Map<String, Object>> create(@PathVariable String requirementId,
                                                       @Valid @RequestBody CreateReviewRequest request) {
        Short seq = request.seq() != null ? request.seq().shortValue() : null;
        return ResponseEntity.ok(reviewService.create(
                requirementId, request.reviewerId(), seq, request.comment(), SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/requirements/{requirementId}/reviews")
    public ResponseEntity<List<Map<String, Object>>> list(@PathVariable String requirementId) {
        return ResponseEntity.ok(reviewService.listByRequirement(requirementId));
    }

    @PostMapping("/reviews/{id}/approve")
    public ResponseEntity<Map<String, Object>> approve(@PathVariable Long id,
                                                        @RequestBody Map<String, String> body) {
        String comment = body != null ? body.get("comment") : null;
        return ResponseEntity.ok(reviewService.approve(id, SecurityUtils.getCurrentUserId(), comment));
    }

    @PostMapping("/reviews/{id}/reject")
    public ResponseEntity<Map<String, Object>> reject(@PathVariable Long id,
                                                       @RequestBody Map<String, String> body) {
        String comment = body != null ? body.get("comment") : null;
        return ResponseEntity.ok(reviewService.reject(id, SecurityUtils.getCurrentUserId(), comment));
    }

    @PostMapping("/reviews/{id}/withdraw")
    public ResponseEntity<Map<String, Object>> withdraw(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.withdraw(id, SecurityUtils.getCurrentUserId()));
    }

}
