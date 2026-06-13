package com.cloudys.auth.service;

import com.cloudys.auth.entity.AuthUser;
import com.cloudys.auth.repository.AuthUserRepository;
import com.cloudys.common.core.constant.Constants;
import com.cloudys.common.core.exception.ErrorResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class RegistrationService {

    private final AuthUserRepository userRepository;

    private static final Set<String> VALID_ROLES = Set.of(
            Constants.ROLE_SUPER_ADMIN, Constants.ROLE_ADMIN,
            Constants.ROLE_MEMBER, Constants.ROLE_VIEWER);

    public RegistrationService(AuthUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listRegistrations(String status, int limit, int offset) {
        List<AuthUser> users;
        if (status != null && !status.isBlank()) {
            users = userRepository.findByRegistrationStatus(status);
        } else {
            List<AuthUser> pending = userRepository.findByRegistrationStatus("pending");
            List<AuthUser> rejected = userRepository.findByRegistrationStatus("rejected");
            users = new ArrayList<>();
            users.addAll(pending);
            users.addAll(rejected);
            users.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        }

        // Manual pagination
        int end = Math.min(users.size(), offset + limit);
        if (offset >= users.size()) return List.of();
        return users.subList(offset, end).stream().map(this::toRegistrationMap).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRegistrationDetail(String userId) {
        AuthUser user = userRepository.findByIdIncludeInactive(userId)
                .orElseThrow(() -> new ErrorResponse("用户不存在", 404));
        return toRegistrationMap(user);
    }

    @Transactional
    public Map<String, Object> approveRegistration(String userId, String role, String externalType, String approvedBy) {
        AuthUser user = userRepository.findByIdIncludeInactive(userId)
                .orElseThrow(() -> new ErrorResponse("用户不存在", 404));

        if (!"pending".equals(user.getRegistrationStatus())) {
            throw new ErrorResponse("该注册申请已处理", 400);
        }

        String targetRole = role != null ? role : Constants.ROLE_MEMBER;
        if (!VALID_ROLES.contains(targetRole)) {
            throw new ErrorResponse("无效角色: " + targetRole, 400);
        }

        user.setRole(targetRole);
        user.setExternalType(externalType);
        user.setIsActive(true);
        user.setRegistrationStatus("approved");
        user.setApprovedAt(Instant.now());
        user.setApprovedBy(approvedBy);
        userRepository.save(user);

        return toRegistrationMap(user);
    }

    @Transactional
    public Map<String, Object> rejectRegistration(String userId, String reason, String rejectedBy) {
        AuthUser user = userRepository.findByIdIncludeInactive(userId)
                .orElseThrow(() -> new ErrorResponse("用户不存在", 404));

        if (!"pending".equals(user.getRegistrationStatus())) {
            throw new ErrorResponse("该注册申请已处理", 400);
        }

        if (reason == null || reason.isBlank()) {
            throw new ErrorResponse("拒绝原因不能为空", 400);
        }

        user.setRegistrationStatus("rejected");
        user.setRejectionReason(reason.trim());
        user.setRejectedAt(Instant.now());
        user.setRejectedBy(rejectedBy);
        userRepository.save(user);

        return toRegistrationMap(user);
    }

    @Transactional(readOnly = true)
    public long getPendingCount() {
        return userRepository.countByRegistrationStatus("pending");
    }

    private Map<String, Object> toRegistrationMap(AuthUser u) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("user_id", u.getUserId());
        map.put("username", u.getUsername());
        map.put("display_name", u.getDisplayName());
        map.put("role", u.getRole());
        map.put("external_type", u.getExternalType());
        map.put("registration_status", u.getRegistrationStatus());
        map.put("rejection_reason", u.getRejectionReason());
        map.put("rejected_at", u.getRejectedAt());
        map.put("rejected_by", u.getRejectedBy());
        map.put("approved_at", u.getApprovedAt());
        map.put("approved_by", u.getApprovedBy());
        map.put("created_at", u.getCreatedAt());
        return map;
    }
}
