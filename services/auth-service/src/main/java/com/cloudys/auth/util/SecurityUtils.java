package com.cloudys.auth.util;

import com.cloudys.common.core.exception.ErrorResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全认证工具类，提供 Controller 层共用的认证辅助方法。
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * 从 SecurityContext 获取当前认证用户的 ID。
     *
     * @return 当前用户 ID
     * @throws ErrorResponse 未认证时抛出 401
     */
    public static String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof String) {
            return (String) auth.getPrincipal();
        }
        throw new ErrorResponse("未认证", 401);
    }
}
