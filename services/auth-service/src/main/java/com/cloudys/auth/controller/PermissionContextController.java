package com.cloudys.auth.controller;

import com.cloudys.auth.service.PermissionService;
import com.cloudys.auth.util.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v2/permission")
public class PermissionContextController {

    private final PermissionService permissionService;

    public PermissionContextController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping("/projects/{projectId}/context")
    public ResponseEntity<Map<String, Object>> getContext(@PathVariable String projectId) {
        return ResponseEntity.ok(permissionService.getUserContext(SecurityUtils.getCurrentUserId(), projectId));
    }

}
