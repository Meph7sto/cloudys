package com.cloudys.auth.service;

import com.cloudys.auth.entity.AuthUser;
import com.cloudys.auth.repository.AuthUserRepository;
import com.cloudys.common.core.constant.Constants;
import com.cloudys.common.core.exception.ErrorResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class UserService {

    private final AuthUserRepository userRepository;
    private final AuthService authService;

    private static final Set<String> VALID_ROLES = Set.of(
            Constants.ROLE_SUPER_ADMIN, Constants.ROLE_ADMIN,
            Constants.ROLE_MEMBER, Constants.ROLE_VIEWER);

    public UserService(AuthUserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public AuthUser getById(String userId) {
        return userRepository.findById(userId)
                .filter(AuthUser::isActive)
                .orElseThrow(() -> new ErrorResponse("用户不存在", 404));
    }

    @Transactional(readOnly = true)
    public AuthUser getByIdIncludeInactive(String userId) {
        return userRepository.findByIdIncludeInactive(userId)
                .orElseThrow(() -> new ErrorResponse("用户不存在", 404));
    }

    @Transactional(readOnly = true)
    public Optional<AuthUser> getByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional
    public AuthUser createUser(String username, String password, String displayName,
                                String role, String externalType, String createdBy) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new ErrorResponse("用户名和密码不能为空", 400);
        }
        if (password.length() < 6) {
            throw new ErrorResponse("密码长度不能少于 6 位", 400);
        }
        if (userRepository.findByUsername(username.trim()).isPresent()) {
            throw new ErrorResponse("用户名已存在", 400);
        }

        AuthUser creator = getById(createdBy);
        String targetRole = role != null ? role : Constants.ROLE_MEMBER;

        validateCanCreateUser(creator, targetRole);

        AuthUser user = new AuthUser();
        user.setUserId(authService.generateUserId());
        user.setUsername(username.trim());
        user.setPasswordHash(authService.hashPassword(password));
        user.setDisplayName(displayName != null ? displayName.trim() : username.trim());
        user.setRole(targetRole);
        user.setExternalType(externalType);
        user.setIsActive(true);
        user.setRegistrationStatus("approved");
        user.setApprovedAt(java.time.Instant.now());
        user.setApprovedBy(createdBy);

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listUsers(String role, boolean includeInactive) {
        List<AuthUser> users = userRepository.listUsers(
                (role != null && !role.isBlank()) ? role : null, includeInactive);
        return users.stream().map(this::toUserMap).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listUserDirectory() {
        List<AuthUser> users = userRepository.listUsers(null, false);
        return users.stream()
                .map(u -> Map.<String, Object>of(
                        "user_id", u.getUserId(),
                        "username", u.getUsername(),
                        "display_name", u.getDisplayName(),
                        "role", u.getRole()))
                .toList();
    }

    @Transactional
    public AuthUser updateUser(String userId, String displayName, String role,
                                Boolean isActive, String updatedBy) {
        AuthUser target = getByIdIncludeInactive(userId);
        AuthUser updater = getById(updatedBy);

        validateCanManageTarget(updater, target);

        if (displayName != null && !displayName.isBlank()) {
            target.setDisplayName(displayName.trim());
        }
        if (role != null && !role.isBlank()) {
            if (!VALID_ROLES.contains(role)) {
                throw new ErrorResponse("无效角色: " + role, 400);
            }
            // super_admin can set any role; admin can only set member/viewer
            if (updater.isAdmin() && !updater.isSuperAdmin()
                    && (Constants.ROLE_SUPER_ADMIN.equals(role) || Constants.ROLE_ADMIN.equals(role))) {
                throw new ErrorResponse("权限不足：管理员只能设置 member 或 viewer 角色", 403);
            }
            target.setRole(role);
        }
        if (isActive != null) {
            target.setIsActive(isActive);
        }

        return userRepository.save(target);
    }

    @Transactional
    public void updateProfile(String userId, String displayName, String avatarUrl) {
        AuthUser user = getById(userId);
        if (displayName != null && !displayName.isBlank()) {
            user.setDisplayName(displayName.trim());
        }
        if (avatarUrl != null) {
            user.setAvatarUrl(avatarUrl.isBlank() ? null : avatarUrl.trim());
        }
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(String userId, String oldPassword, String newPassword) {
        AuthUser user = getById(userId);
        if (!authService.verifyPassword(oldPassword, user.getPasswordHash())) {
            throw new ErrorResponse("旧密码错误", 400);
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new ErrorResponse("新密码长度不能少于 6 位", 400);
        }
        user.setPasswordHash(authService.hashPassword(newPassword));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public long countUsers(String role) {
        return userRepository.countUsers(role);
    }

    void validateCanCreateUser(AuthUser creator, String targetRole) {
        if (creator.isSuperAdmin()) return; // can create any role
        if (creator.isAdmin()) {
            if (Constants.ROLE_SUPER_ADMIN.equals(targetRole) || Constants.ROLE_ADMIN.equals(targetRole)) {
                throw new ErrorResponse("权限不足：管理员不能创建管理员或超级管理员", 403);
            }
            return;
        }
        throw new ErrorResponse("权限不足", 403);
    }

    void validateCanManageTarget(AuthUser manager, AuthUser target) {
        if (manager.isSuperAdmin()) return; // can manage anyone
        if (manager.isAdmin()) {
            if (target.isSuperAdmin()) {
                throw new ErrorResponse("权限不足：管理员不能管理超级管理员", 403);
            }
            return;
        }
        if (manager.getUserId().equals(target.getUserId())) return; // self
        throw new ErrorResponse("权限不足", 403);
    }

    private Map<String, Object> toUserMap(AuthUser u) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("user_id", u.getUserId());
        map.put("username", u.getUsername());
        map.put("display_name", u.getDisplayName());
        map.put("role", u.getRole());
        map.put("external_type", u.getExternalType());
        map.put("is_active", u.getIsActive());
        map.put("avatar_url", u.getAvatarUrl());
        map.put("registration_status", u.getRegistrationStatus());
        map.put("created_at", u.getCreatedAt());
        map.put("updated_at", u.getUpdatedAt());
        return map;
    }
}
