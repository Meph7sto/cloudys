package com.cloudys.auth.service;

import com.cloudys.auth.entity.AuthUser;
import com.cloudys.auth.repository.AuthUserRepository;
import com.cloudys.common.core.constant.Constants;
import com.cloudys.common.core.exception.ErrorResponse;
import com.cloudys.common.security.JwtTokenProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;

@Service
public class AuthService {

    private final AuthUserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    private static final Set<String> TEST_ROLES = Set.of(
            Constants.ROLE_SUPER_ADMIN, Constants.ROLE_ADMIN,
            Constants.ROLE_MEMBER, Constants.ROLE_VIEWER);

    public AuthService(AuthUserRepository userRepository, JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    // ========================
    // Password utilities
    // ========================

    public String hashPassword(String password) {
        return passwordEncoder.encode(password);
    }

    public boolean verifyPassword(String password, String hash) {
        try {
            return passwordEncoder.matches(password, hash);
        } catch (Exception e) {
            return false;
        }
    }

    public String generateUserId() {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        byte[] randomBytes = new byte[4];
        secureRandom.nextBytes(randomBytes);
        return "user-" + timestamp + "-" + HexFormat.of().formatHex(randomBytes);
    }

    // ========================
    // Authentication
    // ========================

    @Transactional(readOnly = true)
    public Map<String, Object> login(String username, String password, String selectedRole) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new ErrorResponse("用户名和密码不能为空", 400);
        }

        AuthUser user = userRepository.findByUsernameForAuth(username)
                .orElseThrow(() -> new ErrorResponse("用户名或密码错误", 401));

        if (!verifyPassword(password, user.getPasswordHash())) {
            throw new ErrorResponse("用户名或密码错误", 401);
        }

        // Check registration status
        if (user.isPending()) {
            throw new ErrorResponse("注册审核中，请等待管理员审批", 403);
        }
        if (user.isRejected()) {
            String reason = user.getRejectionReason() != null ? user.getRejectionReason() : "未提供原因";
            throw new ErrorResponse("注册申请已被拒绝：" + reason, 403);
        }
        if (!user.isActive()) {
            throw new ErrorResponse("用户已被禁用", 403);
        }

        // Determine effective role (test user can simulate)
        String effectiveRole = user.getRole();
        if ("test".equals(username) && selectedRole != null && TEST_ROLES.contains(selectedRole)) {
            effectiveRole = selectedRole;
        }

        String token = tokenProvider.generateToken(
                user.getUserId(), user.getUsername(), effectiveRole, user.getExternalType());

        return Map.of(
                "user_id", user.getUserId(),
                "username", user.getUsername(),
                "display_name", user.getDisplayName() != null ? user.getDisplayName() : "",
                "role", effectiveRole,
                "external_type", user.getExternalType() != null ? user.getExternalType() : "",
                "token", token
        );
    }

    // ========================
    // Registration
    // ========================

    @Transactional
    public Map<String, Object> register(String username, String password, String displayName) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new ErrorResponse("用户名和密码不能为空", 400);
        }
        if (password.length() < 6) {
            throw new ErrorResponse("密码长度不能少于 6 位", 400);
        }

        if (userRepository.findByUsername(username.trim()).isPresent()) {
            throw new ErrorResponse("用户名已存在", 400);
        }

        String passwordHash = hashPassword(password);
        String userId = generateUserId();

        AuthUser user = new AuthUser();
        user.setUserId(userId);
        user.setUsername(username.trim());
        user.setPasswordHash(passwordHash);
        user.setDisplayName(displayName != null ? displayName.trim() : username.trim());
        user.setRole(Constants.ROLE_VIEWER);
        user.setIsActive(false);
        user.setRegistrationStatus("pending");

        userRepository.save(user);

        return Map.of(
                "user_id", user.getUserId(),
                "username", user.getUsername(),
                "display_name", user.getDisplayName(),
                "registration_status", "pending",
                "message", "注册成功，请等待管理员审核"
        );
    }

    // ========================
    // Token verification
    // ========================

    public Map<String, Object> verifyToken(String token) {
        var claims = tokenProvider.verifyToken(token);
        if (claims == null) return null;
        return Map.of(
                "user_id", claims.get("user_id", String.class),
                "username", claims.get("username", String.class),
                "role", claims.get("role", String.class),
                "external_type", claims.get("external_type", String.class)
        );
    }
}
