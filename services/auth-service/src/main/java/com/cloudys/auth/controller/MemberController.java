package com.cloudys.auth.controller;

import com.cloudys.auth.dto.request.AddProjectMemberRequest;
import com.cloudys.auth.dto.request.SetUserRolesRequest;
import com.cloudys.auth.service.MemberService;
import com.cloudys.auth.service.ReviewService;
import com.cloudys.auth.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/permission")
public class MemberController {

    private final MemberService memberService;
    private final ReviewService reviewService;

    public MemberController(MemberService memberService, ReviewService reviewService) {
        this.memberService = memberService;
        this.reviewService = reviewService;
    }

    @PostMapping("/projects/{projectId}/members")
    public ResponseEntity<Map<String, Object>> addMember(@PathVariable String projectId,
                                                          @Valid @RequestBody AddProjectMemberRequest request) {
        return ResponseEntity.ok(memberService.addMember(
                projectId, request.userId(), request.memberRoles(), SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/projects/{projectId}/members")
    public ResponseEntity<List<Map<String, Object>>> listMembers(@PathVariable String projectId) {
        return ResponseEntity.ok(memberService.listMembers(projectId));
    }

    @DeleteMapping("/projects/{projectId}/members/{userId}")
    public ResponseEntity<Map<String, Object>> removeMember(@PathVariable String projectId,
                                                             @PathVariable String userId) {
        memberService.removeMember(projectId, userId);
        return ResponseEntity.ok(Map.of("message", "成员已移除"));
    }

    @PutMapping("/projects/{projectId}/members/{userId}/roles")
    public ResponseEntity<Map<String, Object>> setUserRoles(@PathVariable String projectId,
                                                             @PathVariable String userId,
                                                             @Valid @RequestBody SetUserRolesRequest request) {
        return ResponseEntity.ok(memberService.setUserRoles(
                projectId, userId, request.roles(), SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/user/pending-reviews")
    public ResponseEntity<List<Map<String, Object>>> getUserPendingReviews() {
        return ResponseEntity.ok(reviewService.getUserPendingReviews(SecurityUtils.getCurrentUserId()));
    }

}
