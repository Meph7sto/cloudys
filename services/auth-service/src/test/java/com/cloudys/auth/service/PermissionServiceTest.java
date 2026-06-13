package com.cloudys.auth.service;

import com.cloudys.auth.entity.AuthUser;
import com.cloudys.auth.entity.ProjectMember;
import com.cloudys.auth.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private MemberService memberService;
    @Mock
    private UserProductScopeRepository productScopeRepo;
    @Mock
    private UserProjectScopeRepository projectScopeRepo;
    @Mock
    private ProjectMemberRepository memberRepo;
    @Mock
    private MemberRoleRepository memberRoleRepo;

    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        permissionService = new PermissionService(userService, memberService,
                productScopeRepo, projectScopeRepo, memberRepo, memberRoleRepo);
    }

    private AuthUser createUser(String role) {
        AuthUser user = new AuthUser();
        user.setUserId("user-1");
        user.setUsername("test");
        user.setRole(role);
        user.setIsActive(true);
        user.setRegistrationStatus("approved");
        return user;
    }

    @Nested
    @DisplayName("getSystemIdentity")
    class GetSystemIdentity {

        @Test
        @DisplayName("super_admin has SYS_ADMIN identity")
        void superAdminIdentity() {
            when(userService.getById("user-1")).thenReturn(createUser("super_admin"));
            Map<String, Object> result = permissionService.getSystemIdentity("user-1");
            assertEquals("SYS_ADMIN", result.get("identity"));
        }

        @Test
        @DisplayName("member has SYS_USER identity")
        void memberIdentity() {
            when(userService.getById("user-1")).thenReturn(createUser("member"));
            Map<String, Object> result = permissionService.getSystemIdentity("user-1");
            assertEquals("SYS_USER", result.get("identity"));
        }
    }

    @Nested
    @DisplayName("getUserPermissions")
    class GetUserPermissions {

        @Test
        @DisplayName("super_admin gets all permissions")
        void superAdminAllPermissions() {
            when(userService.getById("user-1")).thenReturn(createUser("super_admin"));

            Set<String> perms = permissionService.getUserPermissions("user-1", "p1");
            assertTrue(perms.contains(PermissionService.PERM_CREATE_PROJECT));
            assertTrue(perms.contains(PermissionService.PERM_MANAGE_MEMBERS));
            assertTrue(perms.contains(PermissionService.PERM_VIEW_PROJECT));
        }

        @Test
        @DisplayName("DEV role gets correct permissions")
        void devRolePermissions() {
            when(userService.getById("user-1")).thenReturn(createUser("member"));
            ProjectMember member = new ProjectMember();
            member.setId(1L);
            when(memberRepo.findByProjectIdAndUserId("p1", "user-1"))
                    .thenReturn(Optional.of(member));
            when(memberRoleRepo.findRoleIdsByMemberId(1L)).thenReturn(List.of("DEV"));

            Set<String> perms = permissionService.getUserPermissions("user-1", "p1");
            assertTrue(perms.contains(PermissionService.PERM_VIEW_PROJECT));
            assertTrue(perms.contains(PermissionService.PERM_EDIT_REQUIREMENT));
            assertTrue(perms.contains(PermissionService.PERM_SUBMIT_REVIEW));
            // DEV should NOT have manage_members
            assertFalse(perms.contains(PermissionService.PERM_MANAGE_MEMBERS));
        }

        @Test
        @DisplayName("unknown user gets minimal permissions")
        void unknownUserPermissions() {
            when(userService.getById("user-1")).thenReturn(createUser("member"));
            when(memberRepo.findByProjectIdAndUserId("p1", "user-1"))
                    .thenReturn(Optional.empty());

            Set<String> perms = permissionService.getUserPermissions("user-1", "p1");
            assertTrue(perms.isEmpty());
        }
    }

    @Nested
    @DisplayName("checkPermission")
    class CheckPermission {

        @Test
        @DisplayName("admin can create projects")
        void adminCanCreateProject() {
            when(userService.getById("user-1")).thenReturn(createUser("admin"));

            assertTrue(permissionService.checkPermission("user-1", "p1",
                    PermissionService.PERM_CREATE_PROJECT));
        }

        @Test
        @DisplayName("CONTRACTOR cannot create requirements")
        void contractorCannotCreateRequirement() {
            when(userService.getById("user-1")).thenReturn(createUser("member"));
            ProjectMember member = new ProjectMember();
            member.setId(1L);
            when(memberRepo.findByProjectIdAndUserId("p1", "user-1"))
                    .thenReturn(Optional.of(member));
            when(memberRoleRepo.findRoleIdsByMemberId(1L)).thenReturn(List.of("CONTRACTOR"));

            assertFalse(permissionService.checkPermission("user-1", "p1",
                    PermissionService.PERM_CREATE_REQUIREMENT));
        }
    }

    @Nested
    @DisplayName("canAccessProject")
    class CanAccessProject {

        @Test
        @DisplayName("admin can access any project")
        void adminAccessAny() {
            when(userService.getById("user-1")).thenReturn(createUser("super_admin"));
            assertTrue(permissionService.canAccessProject("user-1", "p-unknown"));
        }

        @Test
        @DisplayName("member can access project they belong to")
        void memberAccessOwnProject() {
            when(userService.getById("user-1")).thenReturn(createUser("member"));
            when(memberRepo.existsByProjectIdAndUserId("p1", "user-1")).thenReturn(true);

            assertTrue(permissionService.canAccessProject("user-1", "p1"));
        }
    }

    @Nested
    @DisplayName("getUserRoles")
    class GetUserRoles {

        @Test
        @DisplayName("should return project roles")
        void getUserRoles() {
            ProjectMember member = new ProjectMember();
            member.setId(1L);
            when(memberRepo.findByProjectIdAndUserId("p1", "user-1"))
                    .thenReturn(Optional.of(member));
            when(memberRoleRepo.findRoleIdsByMemberId(1L)).thenReturn(List.of("PO", "BA"));

            List<String> roles = permissionService.getUserRoles("user-1", "p1");
            assertEquals(2, roles.size());
            assertTrue(roles.contains("PO"));
            assertTrue(roles.contains("BA"));
        }

        @Test
        @DisplayName("should return empty for non-member")
        void getUserRolesNonMember() {
            when(memberRepo.findByProjectIdAndUserId("p1", "user-1"))
                    .thenReturn(Optional.empty());

            List<String> roles = permissionService.getUserRoles("user-1", "p1");
            assertTrue(roles.isEmpty());
        }
    }
}
