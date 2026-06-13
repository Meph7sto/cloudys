package com.cloudys.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * RBAC 角色检查辅助注解。
 * 对应 Python 的 require_roles / require_admin / require_member_or_admin。
 *
 * 使用示例:
 * <pre>{@code
 * @RoleCheck.Admin
 * @GetMapping("/admin-only")
 * public ResponseEntity<?> adminOnly() { ... }
 * }</pre>
 */
public final class RoleCheck {

    private RoleCheck() {}

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @PreAuthorize("hasRole('super_admin') or hasRole('admin')")
    public @interface Admin {}

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @PreAuthorize("hasRole('super_admin')")
    public @interface SuperAdmin {}

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @PreAuthorize("hasRole('admin') or hasRole('member')")
    public @interface MemberOrAdmin {}
}
