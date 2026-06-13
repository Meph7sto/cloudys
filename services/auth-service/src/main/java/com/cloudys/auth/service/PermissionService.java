package com.cloudys.auth.service;

import com.cloudys.auth.entity.AuthUser;
import com.cloudys.auth.repository.*;
import com.cloudys.common.core.constant.Constants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class PermissionService {

    private final UserService userService;
    private final MemberService memberService;
    private final UserProductScopeRepository productScopeRepo;
    private final UserProjectScopeRepository projectScopeRepo;
    private final ProjectMemberRepository memberRepo;
    private final MemberRoleRepository memberRoleRepo;

    // ========================
    // 21 Permission Points (matching Python exactly)
    // ========================
    public static final String PERM_CREATE_PROJECT = "create_project";
    public static final String PERM_EDIT_PROJECT = "edit_project";
    public static final String PERM_DELETE_PROJECT = "delete_project";
    public static final String PERM_VIEW_PROJECT = "view_project";
    public static final String PERM_CREATE_PRODUCT = "create_product";
    public static final String PERM_EDIT_PRODUCT = "edit_product";
    public static final String PERM_DELETE_PRODUCT = "delete_product";
    public static final String PERM_VIEW_PRODUCT = "view_product";
    public static final String PERM_CREATE_REQUIREMENT = "create_requirement";
    public static final String PERM_EDIT_REQUIREMENT = "edit_requirement";
    public static final String PERM_DELETE_REQUIREMENT = "delete_requirement";
    public static final String PERM_VIEW_REQUIREMENT = "view_requirement";
    public static final String PERM_MANAGE_MEMBERS = "manage_members";
    public static final String PERM_MANAGE_ROLES = "manage_roles";
    public static final String PERM_CREATE_BASELINE = "create_baseline";
    public static final String PERM_LOCK_BASELINE = "lock_baseline";
    public static final String PERM_CREATE_CHANGE_REQUEST = "create_change_request";
    public static final String PERM_APPROVE_CHANGE_REQUEST = "approve_change_request";
    public static final String PERM_ASSIGN_REVIEWER = "assign_reviewer";
    public static final String PERM_SUBMIT_REVIEW = "submit_review";
    public static final String PERM_VIEW_REVIEW = "view_review";

    // ========================
    // Project Roles
    // ========================
    public static final String ROLE_PO = "PO";
    public static final String ROLE_BA = "BA";
    public static final String ROLE_DEV = "DEV";
    public static final String ROLE_REVIEWER = "REVIEWER";
    public static final String ROLE_QA = "QA";
    public static final String ROLE_CONTRACTOR = "CONTRACTOR";
    public static final String ROLE_CLIENT = "CLIENT";

    public static final List<String> ALL_ROLES = List.of(
            ROLE_PO, ROLE_BA, ROLE_DEV, ROLE_REVIEWER, ROLE_QA, ROLE_CONTRACTOR, ROLE_CLIENT);

    // ========================
    // Role-to-Permission Mapping
    // ========================
    private static final Map<String, Set<String>> ROLE_PERMISSIONS = Map.of(
            ROLE_PO, Set.of(
                    PERM_VIEW_PROJECT, PERM_EDIT_PROJECT, PERM_DELETE_PROJECT,
                    PERM_VIEW_PRODUCT, PERM_EDIT_PRODUCT,
                    PERM_CREATE_REQUIREMENT, PERM_EDIT_REQUIREMENT, PERM_DELETE_REQUIREMENT, PERM_VIEW_REQUIREMENT,
                    PERM_MANAGE_MEMBERS, PERM_MANAGE_ROLES,
                    PERM_CREATE_BASELINE, PERM_LOCK_BASELINE,
                    PERM_CREATE_CHANGE_REQUEST, PERM_APPROVE_CHANGE_REQUEST,
                    PERM_ASSIGN_REVIEWER, PERM_VIEW_REVIEW
            ),
            ROLE_BA, Set.of(
                    PERM_VIEW_PROJECT,
                    PERM_VIEW_PRODUCT,
                    PERM_CREATE_REQUIREMENT, PERM_EDIT_REQUIREMENT, PERM_VIEW_REQUIREMENT,
                    PERM_CREATE_CHANGE_REQUEST,
                    PERM_ASSIGN_REVIEWER, PERM_VIEW_REVIEW
            ),
            ROLE_DEV, Set.of(
                    PERM_VIEW_PROJECT,
                    PERM_VIEW_PRODUCT,
                    PERM_VIEW_REQUIREMENT, PERM_EDIT_REQUIREMENT,
                    PERM_CREATE_CHANGE_REQUEST,
                    PERM_SUBMIT_REVIEW, PERM_VIEW_REVIEW
            ),
            ROLE_REVIEWER, Set.of(
                    PERM_VIEW_PROJECT,
                    PERM_VIEW_REQUIREMENT,
                    PERM_SUBMIT_REVIEW, PERM_VIEW_REVIEW
            ),
            ROLE_QA, Set.of(
                    PERM_VIEW_PROJECT,
                    PERM_VIEW_PRODUCT,
                    PERM_VIEW_REQUIREMENT, PERM_EDIT_REQUIREMENT,
                    PERM_CREATE_CHANGE_REQUEST,
                    PERM_SUBMIT_REVIEW, PERM_VIEW_REVIEW
            ),
            ROLE_CONTRACTOR, Set.of(
                    PERM_VIEW_PROJECT,
                    PERM_VIEW_REQUIREMENT,
                    PERM_VIEW_REVIEW
            ),
            ROLE_CLIENT, Set.of(
                    PERM_VIEW_PROJECT,
                    PERM_VIEW_PRODUCT,
                    PERM_VIEW_REQUIREMENT,
                    PERM_CREATE_CHANGE_REQUEST,
                    PERM_VIEW_REVIEW
            )
    );

    public PermissionService(UserService userService, MemberService memberService,
                             UserProductScopeRepository productScopeRepo,
                             UserProjectScopeRepository projectScopeRepo,
                             ProjectMemberRepository memberRepo,
                             MemberRoleRepository memberRoleRepo) {
        this.userService = userService;
        this.memberService = memberService;
        this.productScopeRepo = productScopeRepo;
        this.projectScopeRepo = projectScopeRepo;
        this.memberRepo = memberRepo;
        this.memberRoleRepo = memberRoleRepo;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSystemIdentity(String userId) {
        AuthUser user = userService.getById(userId);
        if (user.isAdmin()) {
            return Map.of("identity", "SYS_ADMIN", "user_id", userId, "role", user.getRole());
        }
        return Map.of("identity", "SYS_USER", "user_id", userId, "role", user.getRole());
    }

    @Transactional(readOnly = true)
    public Set<String> getUserPermissions(String userId, String projectId) {
        AuthUser user = userService.getById(userId);

        // Super admin gets all permissions
        if (user.isSuperAdmin()) {
            return allPermissions();
        }

        Set<String> permissions = new HashSet<>();
        if (user.isAdmin()) {
            permissions.addAll(adminPermissions());
        }

        // Get project roles
        if (projectId != null) {
            memberRepo.findByProjectIdAndUserId(projectId, userId).ifPresent(member -> {
                List<String> roles = getMemberRoles(member.getId());
                for (String role : roles) {
                    Set<String> perms = ROLE_PERMISSIONS.get(role);
                    if (perms != null) permissions.addAll(perms);
                }
            });
        }

        return permissions;
    }

    @Transactional(readOnly = true)
    public boolean checkPermission(String userId, String projectId, String permission) {
        Set<String> permissions = getUserPermissions(userId, projectId);
        return permissions.contains(permission);
    }

    @Transactional(readOnly = true)
    public List<String> getUserRoles(String userId, String projectId) {
        if (projectId == null) return List.of();

        var memberOpt = memberRepo.findByProjectIdAndUserId(projectId, userId);
        if (memberOpt.isEmpty()) return List.of();

        return getMemberRoles(memberOpt.get().getId());
    }

    @Transactional(readOnly = true)
    public boolean canAccessProject(String userId, String projectId) {
        AuthUser user = userService.getById(userId);
        if (user.isAdmin()) return true;

        // Explicit project scope
        if (projectScopeRepo.existsByUserIdAndProjectId(userId, projectId)) {
            return true;
        }

        // Derived access via product scope
        List<String> productIds = productScopeRepo.findProductIdsByUserId(userId);
        if (!productIds.isEmpty()) {
            // Check if any product's project matches
            // (This would need a JOIN with manage_projects, but we simplify)
            return false; // Placeholder — would need project-service data
        }

        // Check if user is a project member
        return memberRepo.existsByProjectIdAndUserId(projectId, userId);
    }

    @Transactional(readOnly = true)
    public boolean canAccessProduct(String userId, String productId) {
        AuthUser user = userService.getById(userId);
        if (user.isAdmin()) return true;

        return productScopeRepo.existsByUserIdAndProductId(userId, productId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserContext(String userId, String projectId) {
        AuthUser user = userService.getById(userId);

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("user_id", userId);
        context.put("username", user.getUsername());
        context.put("role", user.getRole());
        context.put("external_type", user.getExternalType());

        if (user.isAdmin()) {
            context.put("permissions", allPermissions());
            context.put("can_access", true);
            context.put("can_edit", true);
        } else {
            Set<String> permissions = getUserPermissions(userId, projectId);
            context.put("permissions", permissions);
            context.put("can_access", canAccessProject(userId, projectId));
            context.put("can_edit", projectScopeRepo.findCanEditByUserIdAndProjectId(userId, projectId).orElse(false));
        }

        if (projectId != null) {
            context.put("project_roles", getUserRoles(userId, projectId));
        }

        return context;
    }

    @Transactional(readOnly = true)
    public boolean canEditProject(String userId, String projectId) {
        AuthUser user = userService.getById(userId);
        if (user.isAdmin()) return true;
        return projectScopeRepo.findCanEditByUserIdAndProjectId(userId, projectId).orElse(false);
    }

    @Transactional(readOnly = true)
    public List<String> getAccessibleProjects(String userId) {
        AuthUser user = userService.getById(userId);
        if (user.isAdmin()) return List.of(); // All projects accessible — caller should query all

        Set<String> projectIds = new LinkedHashSet<>();
        projectIds.addAll(projectScopeRepo.findProjectIdsByUserId(userId));
        projectIds.addAll(memberRepo.findProjectIdsByUserId(userId));
        return new ArrayList<>(projectIds);
    }

    private List<String> getMemberRoles(Long memberId) {
        return memberRoleRepo.findRoleIdsByMemberId(memberId);
    }

    private static Set<String> allPermissions() {
        Set<String> all = new HashSet<>(adminPermissions());
        all.addAll(ROLE_PERMISSIONS.getOrDefault(ROLE_PO, Set.of()));
        return all;
    }

    private static Set<String> adminPermissions() {
        return Set.of(
                PERM_CREATE_PROJECT, PERM_EDIT_PROJECT, PERM_DELETE_PROJECT, PERM_VIEW_PROJECT,
                PERM_CREATE_PRODUCT, PERM_EDIT_PRODUCT, PERM_DELETE_PRODUCT, PERM_VIEW_PRODUCT,
                PERM_CREATE_REQUIREMENT, PERM_EDIT_REQUIREMENT, PERM_DELETE_REQUIREMENT, PERM_VIEW_REQUIREMENT,
                PERM_MANAGE_MEMBERS, PERM_MANAGE_ROLES,
                PERM_CREATE_BASELINE, PERM_LOCK_BASELINE,
                PERM_CREATE_CHANGE_REQUEST, PERM_APPROVE_CHANGE_REQUEST,
                PERM_ASSIGN_REVIEWER, PERM_VIEW_REVIEW
        );
    }
}
