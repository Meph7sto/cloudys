package com.cloudys.auth.controller;

import com.cloudys.auth.dto.request.*;
import com.cloudys.auth.entity.AuthUser;
import com.cloudys.auth.service.*;
import com.cloudys.auth.util.SecurityUtils;
import com.cloudys.common.dto.auth.LoginRequest;
import com.cloudys.common.dto.auth.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final RegistrationService registrationService;
    private final ScopeService scopeService;

    public AuthController(AuthService authService, UserService userService,
                          RegistrationService registrationService, ScopeService scopeService) {
        this.authService = authService;
        this.userService = userService;
        this.registrationService = registrationService;
        this.scopeService = scopeService;
    }

    // ========================
    // Public endpoints
    // ========================

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request,
                                                      @RequestParam(required = false) String role) {
        return ResponseEntity.ok(authService.login(request.username(), request.password(), role));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(
                request.username(), request.password(), request.displayName()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        return ResponseEntity.ok(Map.of("message", "已退出登录"));
    }

    // ========================
    // Authenticated endpoints (shared)
    // ========================

    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify() {
        String currentUserId = SecurityUtils.getCurrentUserId();
        var user = userService.getById(currentUserId);
        return ResponseEntity.ok(toUserMap(user, false));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {
        String currentUserId = SecurityUtils.getCurrentUserId();
        var user = userService.getById(currentUserId);
        return ResponseEntity.ok(toUserMap(user, true));
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile() {
        return me();
    }

    @PatchMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestBody UpdateProfileRequest request) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        userService.updateProfile(currentUserId, request.displayName(), request.avatarUrl());
        return me();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        userService.changePassword(currentUserId, request.oldPassword(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "密码修改成功"));
    }

    @PostMapping("/profile/change-password")
    public ResponseEntity<Map<String, Object>> changeProfilePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        return changePassword(request);
    }

    @GetMapping("/user-directory")
    public ResponseEntity<List<Map<String, Object>>> userDirectory() {
        return ResponseEntity.ok(userService.listUserDirectory());
    }

    // ========================
    // Admin endpoints — User management
    // ========================

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> listUsers(
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(userService.listUsers(role, includeInactive));
    }

    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> createUser(@Valid @RequestBody CreateUserRequest request) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        var user = userService.createUser(request.username(), request.password(),
                request.displayName(), request.role(), request.externalType(), currentUserId);
        return ResponseEntity.ok(Map.of(
                "user_id", user.getUserId(),
                "username", user.getUsername(),
                "display_name", user.getDisplayName(),
                "role", user.getRole(),
                "external_type", user.getExternalType(),
                "is_active", user.getIsActive()
        ));
    }

    @PatchMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable String userId,
                                                           @RequestBody UpdateUserRequest request) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        var user = userService.updateUser(userId, request.displayName(), request.role(),
                request.isActive(), currentUserId);
        return ResponseEntity.ok(Map.of(
                "user_id", user.getUserId(),
                "username", user.getUsername(),
                "display_name", user.getDisplayName(),
                "role", user.getRole(),
                "external_type", user.getExternalType(),
                "is_active", user.getIsActive()
        ));
    }

    // ========================
    // Admin endpoints — Scopes
    // ========================

    @GetMapping("/scope-options")
    public ResponseEntity<Map<String, Object>> getScopeOptions() {
        return ResponseEntity.ok(scopeService.getScopeOptions());
    }

    @GetMapping("/user-scopes/{userId}")
    public ResponseEntity<Map<String, Object>> getUserScopes(@PathVariable String userId) {
        return ResponseEntity.ok(scopeService.getUserScopes(userId));
    }

    @GetMapping("/users/{userId}/scopes")
    public ResponseEntity<Map<String, Object>> getUserScopesCompat(@PathVariable String userId) {
        return getUserScopes(userId);
    }

    @PutMapping("/user-scopes/{userId}")
    public ResponseEntity<Map<String, Object>> updateUserScopes(@PathVariable String userId,
                                                                  @RequestBody UpdateUserScopesRequest request) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(scopeService.replaceUserScopes(userId, request, currentUserId));
    }

    @PutMapping("/users/{userId}/scopes")
    public ResponseEntity<Map<String, Object>> updateUserScopesCompat(@PathVariable String userId,
                                                                       @RequestBody UpdateUserScopesRequest request) {
        return updateUserScopes(userId, request);
    }

    // ========================
    // Admin endpoints — Registration management
    // ========================

    @GetMapping("/registrations")
    public ResponseEntity<List<Map<String, Object>>> listRegistrations(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(registrationService.listRegistrations(status, limit, offset));
    }

    @GetMapping("/registrations/{userId}")
    public ResponseEntity<Map<String, Object>> getRegistrationDetail(@PathVariable String userId) {
        return ResponseEntity.ok(registrationService.getRegistrationDetail(userId));
    }

    @PostMapping("/registrations/{userId}/approve")
    public ResponseEntity<Map<String, Object>> approveRegistration(@PathVariable String userId,
                                                                     @RequestBody ApproveRegistrationRequest request) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(registrationService.approveRegistration(
                userId, request.role(), request.externalType(), currentUserId));
    }

    @PostMapping("/registrations/{userId}/reject")
    public ResponseEntity<Map<String, Object>> rejectRegistration(@PathVariable String userId,
                                                                    @Valid @RequestBody RejectRegistrationRequest request) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(registrationService.rejectRegistration(
                userId, request.reason(), currentUserId));
    }

    @GetMapping("/registrations/pending/count")
    public ResponseEntity<Map<String, Object>> pendingRegistrationCount() {
        return ResponseEntity.ok(Map.of("count", registrationService.getPendingCount()));
    }

    // ========================
    // Helper
    // ========================

    private Map<String, Object> toUserMap(AuthUser user, boolean includeProfileFields) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("user_id", user.getUserId());
        map.put("username", user.getUsername());
        map.put("display_name", user.getDisplayName());
        map.put("role", user.getRole());
        map.put("external_type", user.getExternalType());
        map.put("is_active", user.getIsActive());
        map.put("registration_status", user.getRegistrationStatus());

        if (includeProfileFields) {
            map.put("avatar_url", user.getAvatarUrl());
            map.put("created_at", user.getCreatedAt());
            map.put("updated_at", user.getUpdatedAt());
        }

        return map;
    }
}
