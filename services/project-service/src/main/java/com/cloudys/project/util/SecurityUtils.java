package com.cloudys.project.util;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return "system";
        }
        return authentication.getPrincipal().toString();
    }

    public static String getCurrentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getDetails() instanceof Map<?, ?> details)) {
            return "";
        }
        Object role = details.get("role");
        return role != null ? role.toString() : "";
    }

    public static boolean isAdmin() {
        String role = getCurrentRole();
        return "super_admin".equals(role) || "admin".equals(role);
    }
}
