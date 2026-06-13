package com.cloudys.inference.util;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;

import reactor.core.publisher.Mono;

/**
 * WebFlux 版安全工具类。
 * 使用 ReactiveSecurityContextHolder 获取当前认证信息。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前用户 ID（响应式）。
     */
    public static Mono<String> getCurrentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(SecurityUtils::extractUserId)
                .defaultIfEmpty("system");
    }

    /**
     * 获取当前角色（响应式）。
     */
    public static Mono<String> getCurrentRole() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(SecurityUtils::extractRole)
                .defaultIfEmpty("");
    }

    /**
     * 判断是否为管理员（响应式）。
     */
    public static Mono<Boolean> isAdmin() {
        return getCurrentRole()
                .map(role -> "super_admin".equals(role) || "admin".equals(role));
    }

    // --- blocking helpers (for sync contexts) ---

    private static String extractUserId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            return "system";
        }
        return auth.getPrincipal().toString();
    }

    private static String extractRole(Authentication auth) {
        if (auth == null || !(auth.getDetails() instanceof Map<?, ?> details)) {
            return "";
        }
        Object role = details.get("role");
        return role != null ? role.toString() : "";
    }
}
