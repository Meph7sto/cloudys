package com.cloudys.auth.controller;

import com.cloudys.auth.dto.request.CreateChangeRequestRequest;
import com.cloudys.auth.service.ChangeRequestService;
import com.cloudys.auth.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/permission")
public class ChangeRequestController {

    private final ChangeRequestService crService;

    public ChangeRequestController(ChangeRequestService crService) {
        this.crService = crService;
    }

    @PostMapping("/requirements/{requirementId}/change-requests")
    public ResponseEntity<Map<String, Object>> create(@PathVariable String requirementId,
                                                       @Valid @RequestBody CreateChangeRequestRequest request) {
        return ResponseEntity.ok(crService.create(
                requirementId, request.baselineId(), request.reason(),
                request.changeSummary(), SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/requirements/{requirementId}/change-requests")
    public ResponseEntity<List<Map<String, Object>>> list(@PathVariable String requirementId) {
        return ResponseEntity.ok(crService.listByRequirement(requirementId));
    }

    @PostMapping("/change-requests/{id}/approve")
    public ResponseEntity<Map<String, Object>> approve(@PathVariable Long id,
                                                        @RequestBody Map<String, String> body) {
        String comment = body != null ? body.get("review_comment") : null;
        return ResponseEntity.ok(crService.approve(id, comment, SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/change-requests/{id}/reject")
    public ResponseEntity<Map<String, Object>> reject(@PathVariable Long id,
                                                       @RequestBody Map<String, String> body) {
        String comment = body != null ? body.get("review_comment") : null;
        return ResponseEntity.ok(crService.reject(id, comment, SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/change-requests/pending")
    public ResponseEntity<List<Map<String, Object>>> pending() {
        return ResponseEntity.ok(crService.getPending());
    }

}
