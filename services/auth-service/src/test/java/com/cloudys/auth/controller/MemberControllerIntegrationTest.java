package com.cloudys.auth.controller;

import com.cloudys.auth.entity.AuthUser;
import com.cloudys.auth.entity.MemberRole;
import com.cloudys.auth.entity.ProjectMember;
import com.cloudys.auth.repository.AuthUserRepository;
import com.cloudys.auth.repository.MemberRoleRepository;
import com.cloudys.auth.repository.ProjectMemberRepository;
import com.cloudys.common.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MemberControllerIntegrationTest {

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

    @Autowired
    private ObjectMapper objectMapper;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private String adminToken;
    private String memberToken;
    private AuthUser memberUser;
    private final String projectId = "proj-test-001";

    @BeforeEach
    void setUp() {
        memberRepo.deleteAll();
        roleRepo.deleteAll();
        userRepository.deleteAll();

        AuthUser adminUser = createUser("admin", "admin", "super_admin", "approved");
        adminToken = tokenProvider.generateToken(adminUser.getUserId(), "admin", "super_admin", null);

        memberUser = createUser("member", "member", "member", "approved");
        memberToken = tokenProvider.generateToken(memberUser.getUserId(), "member", "member", null);
    }

    private AuthUser createUser(String username, String password, String role, String status) {
        AuthUser user = new AuthUser();
        user.setUserId("test-" + username);
        user.setUsername(username);
        user.setPasswordHash(encoder.encode(password));
        user.setDisplayName(username);
        user.setRole(role);
        user.setIsActive(true);
        user.setRegistrationStatus(status);
        if ("approved".equals(status)) {
            user.setApprovedAt(Instant.now());
            user.setApprovedBy("system");
        }
        return userRepository.save(user);
    }

    private String json(Map<String, Object> map) throws Exception {
        return objectMapper.writeValueAsString(map);
    }

    @Nested
    @DisplayName("POST /api/v2/permission/projects/{id}/members")
    class AddMember {

        @Test
        @DisplayName("should add a member to a project")
        void addMemberSuccess() throws Exception {
            mockMvc.perform(post("/api/v2/permission/projects/" + projectId + "/members")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "userId", memberUser.getUserId(),
                                    "memberRoles", List.of("DEV", "QA")))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user_id", is(memberUser.getUserId())))
                    .andExpect(jsonPath("$.project_id", is(projectId)))
                    .andExpect(jsonPath("$.roles", hasItems("DEV", "QA")));
        }

        @Test
        @DisplayName("should return 401 without token")
        void addMemberWithoutToken() throws Exception {
            mockMvc.perform(post("/api/v2/permission/projects/" + projectId + "/members")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("userId", "u", "memberRoles", List.of("DEV")))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 400 for duplicate member")
        void addDuplicateMember() throws Exception {
            // First add
            mockMvc.perform(post("/api/v2/permission/projects/" + projectId + "/members")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "userId", memberUser.getUserId(),
                                    "memberRoles", List.of("DEV")))))
                    .andExpect(status().isOk());

            // Second add — should fail
            mockMvc.perform(post("/api/v2/permission/projects/" + projectId + "/members")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "userId", memberUser.getUserId(),
                                    "memberRoles", List.of("QA")))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail", containsString("已是项目成员")));
        }
    }

    @Nested
    @DisplayName("GET /api/v2/permission/projects/{id}/members")
    class ListMembers {

        @Test
        @DisplayName("should list all members of a project")
        void listMembersSuccess() throws Exception {
            // Add a member first
            ProjectMember member = new ProjectMember();
            member.setProjectId(projectId);
            member.setUserId(memberUser.getUserId());
            member = memberRepo.save(member);

            MemberRole role = new MemberRole();
            role.setMemberId(member.getId());
            role.setRoleId("DEV");
            role.setGrantedBy("test-admin");
            roleRepo.save(role);

            mockMvc.perform(get("/api/v2/permission/projects/" + projectId + "/members")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].user_id", is(memberUser.getUserId())))
                    .andExpect(jsonPath("$[0].roles", hasItem("DEV")));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v2/permission/projects/{id}/members/{userId}")
    class RemoveMember {

        @Test
        @DisplayName("should remove a member from a project")
        void removeMemberSuccess() throws Exception {
            ProjectMember member = new ProjectMember();
            member.setProjectId(projectId);
            member.setUserId(memberUser.getUserId());
            member = memberRepo.save(member);

            mockMvc.perform(delete("/api/v2/permission/projects/" + projectId + "/members/" + memberUser.getUserId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", containsString("成员已移除")));
        }
    }

    @Nested
    @DisplayName("PUT /api/v2/permission/projects/{id}/members/{userId}/roles")
    class SetUserRoles {

        @Test
        @DisplayName("should update member roles")
        void setRolesSuccess() throws Exception {
            ProjectMember member = new ProjectMember();
            member.setProjectId(projectId);
            member.setUserId(memberUser.getUserId());
            member = memberRepo.save(member);

            mockMvc.perform(put("/api/v2/permission/projects/" + projectId + "/members/" + memberUser.getUserId() + "/roles")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("roles", List.of("PO", "BA")))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roles", hasItems("PO", "BA")));
        }

        @Test
        @DisplayName("should return 400 for invalid role")
        void setRolesInvalidRole() throws Exception {
            ProjectMember member = new ProjectMember();
            member.setProjectId(projectId);
            member.setUserId(memberUser.getUserId());
            member = memberRepo.save(member);

            mockMvc.perform(put("/api/v2/permission/projects/" + projectId + "/members/" + memberUser.getUserId() + "/roles")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("roles", List.of("INVALID_ROLE")))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail", containsString("无效")));
        }
    }
}
