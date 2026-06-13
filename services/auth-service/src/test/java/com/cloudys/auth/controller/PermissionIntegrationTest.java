package com.cloudys.auth.controller;

import com.cloudys.auth.entity.AuthUser;
import com.cloudys.auth.entity.MemberRole;
import com.cloudys.auth.entity.ProjectMember;
import com.cloudys.auth.repository.AuthUserRepository;
import com.cloudys.auth.repository.MemberRoleRepository;
import com.cloudys.auth.repository.ProjectMemberRepository;
import com.cloudys.common.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PermissionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthUserRepository userRepository;

    @Autowired
    private ProjectMemberRepository memberRepo;

    @Autowired
    private MemberRoleRepository roleRepo;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private String adminToken;
    private String memberToken;
    private final String projectId = "proj-perm-001";

    @BeforeEach
    void setUp() {
        memberRepo.deleteAll();
        roleRepo.deleteAll();
        userRepository.deleteAll();

        // Admin
        AuthUser admin = createUser("admin", "admin", "super_admin");
        adminToken = tokenProvider.generateToken(admin.getUserId(), "admin", "super_admin", null);

        // Member with project role
        AuthUser member = createUser("member", "member", "member");
        memberToken = tokenProvider.generateToken(member.getUserId(), "member", "member", null);

        // Add member to project with DEV role
        ProjectMember pm = new ProjectMember();
        pm.setProjectId(projectId);
        pm.setUserId(member.getUserId());
        pm = memberRepo.save(pm);

        MemberRole role = new MemberRole();
        role.setMemberId(pm.getId());
        role.setRoleId("DEV");
        role.setGrantedBy(admin.getUserId());
        roleRepo.save(role);
    }

    private AuthUser createUser(String username, String password, String role) {
        AuthUser user = new AuthUser();
        user.setUserId("test-" + username);
        user.setUsername(username);
        user.setPasswordHash(encoder.encode(password));
        user.setDisplayName(username);
        user.setRole(role);
        user.setIsActive(true);
        user.setRegistrationStatus("approved");
        user.setApprovedAt(Instant.now());
        user.setApprovedBy("system");
        return userRepository.save(user);
    }

    @Nested
    @DisplayName("GET /api/v2/permission/projects/{id}/context")
    class PermissionContext {

        @Test
        @DisplayName("admin gets full permissions context")
        void adminGetsFullContext() throws Exception {
            mockMvc.perform(get("/api/v2/permission/projects/" + projectId + "/context")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user_id", is("test-admin")))
                    .andExpect(jsonPath("$.can_access", is(true)))
                    .andExpect(jsonPath("$.can_edit", is(true)))
                    .andExpect(jsonPath("$.role", is("super_admin")))
                    .andExpect(jsonPath("$.permissions", hasItem("create_project")));
        }

        @Test
        @DisplayName("member gets project role permissions")
        void memberGetsProjectPermissions() throws Exception {
            mockMvc.perform(get("/api/v2/permission/projects/" + projectId + "/context")
                            .header("Authorization", "Bearer " + memberToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user_id", is("test-member")))
                    .andExpect(jsonPath("$.project_roles", hasItem("DEV")))
                    .andExpect(jsonPath("$.permissions", hasItem("view_project")))
                    .andExpect(jsonPath("$.permissions", hasItem("edit_requirement")));
        }

        @Test
        @DisplayName("should return 401 without token")
        void contextWithoutToken() throws Exception {
            mockMvc.perform(get("/api/v2/permission/projects/" + projectId + "/context"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
