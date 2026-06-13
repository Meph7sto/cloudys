package com.cloudys.auth.service;

import com.cloudys.auth.entity.AuthUser;
import com.cloudys.auth.entity.ProjectMember;
import com.cloudys.auth.repository.MemberRoleRepository;
import com.cloudys.auth.repository.ProjectMemberRepository;
import com.cloudys.common.core.exception.ErrorResponse;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private ProjectMemberRepository memberRepo;

    @Mock
    private MemberRoleRepository roleRepo;

    @Mock
    private UserService userService;

    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberService = new MemberService(memberRepo, roleRepo, userService);
    }

    private ProjectMember createMember(Long id, String projectId, String userId) {
        ProjectMember member = new ProjectMember();
        member.setId(id);
        member.setProjectId(projectId);
        member.setUserId(userId);
        return member;
    }

    @Nested
    @DisplayName("addMember")
    class AddMember {

        @Test
        @DisplayName("should add member with roles")
        void addMemberWithRoles() {
            when(userService.getById("u1")).thenReturn(mock(AuthUser.class));
            when(memberRepo.existsByProjectIdAndUserId("p1", "u1")).thenReturn(false);
            ProjectMember saved = createMember(1L, "p1", "u1");
            when(memberRepo.save(any(ProjectMember.class))).thenReturn(saved);

            Map<String, Object> result = memberService.addMember("p1", "u1",
                    List.of("DEV", "QA"), "grantor");

            assertNotNull(result);
            assertEquals("u1", result.get("user_id"));
            verify(roleRepo, times(2)).save(any());
        }

        @Test
        @DisplayName("should throw for duplicate membership")
        void addDuplicateMember() {
            when(userService.getById("u1")).thenReturn(mock(AuthUser.class));
            when(memberRepo.existsByProjectIdAndUserId("p1", "u1")).thenReturn(true);

            ErrorResponse ex = assertThrows(ErrorResponse.class, () ->
                    memberService.addMember("p1", "u1", List.of("DEV"), "g"));
            assertTrue(ex.getDetail().contains("已是项目成员"));
        }

        @Test
        @DisplayName("should throw for invalid role")
        void addMemberWithInvalidRole() {
            when(userService.getById("u1")).thenReturn(mock(AuthUser.class));
            when(memberRepo.existsByProjectIdAndUserId("p1", "u1")).thenReturn(false);

            ErrorResponse ex = assertThrows(ErrorResponse.class, () ->
                    memberService.addMember("p1", "u1", List.of("INVALID"), "g"));
            assertTrue(ex.getDetail().contains("无效"));
        }
    }

    @Nested
    @DisplayName("removeMember")
    class RemoveMember {

        @Test
        @DisplayName("should remove member and roles")
        void removeMemberSuccess() {
            ProjectMember member = createMember(1L, "p1", "u1");
            when(memberRepo.findByProjectIdAndUserId("p1", "u1")).thenReturn(Optional.of(member));

            memberService.removeMember("p1", "u1");
            verify(roleRepo).deleteAllByMemberId(1L);
            verify(memberRepo).delete(member);
        }

        @Test
        @DisplayName("should throw for non-existent member")
        void removeNonExistentMember() {
            when(memberRepo.findByProjectIdAndUserId("p1", "u1")).thenReturn(Optional.empty());
            assertThrows(ErrorResponse.class, () -> memberService.removeMember("p1", "u1"));
        }
    }

    @Nested
    @DisplayName("setUserRoles")
    class SetUserRoles {

        @Test
        @DisplayName("should do diff-based role update")
        void setRolesDiff() {
            ProjectMember member = createMember(1L, "p1", "u1");
            when(memberRepo.findByProjectIdAndUserId("p1", "u1")).thenReturn(Optional.of(member));
            when(roleRepo.findRoleIdsByMemberId(1L))
                    .thenReturn(List.of("DEV"))  // current
                    .thenReturn(List.of("PO", "QA")); // after save

            Map<String, Object> result = memberService.setUserRoles("p1", "u1",
                    List.of("PO", "QA"), "g");

            verify(roleRepo).deleteByMemberAndRole(1L, "DEV");
            verify(roleRepo, times(2)).save(any());
            assertFalse(result.get("roles").toString().contains("DEV"));
        }

        @Test
        @DisplayName("should throw for invalid role in set")
        void setRolesInvalid() {
            ProjectMember member = createMember(1L, "p1", "u1");
            when(memberRepo.findByProjectIdAndUserId("p1", "u1")).thenReturn(Optional.of(member));

            assertThrows(ErrorResponse.class, () ->
                    memberService.setUserRoles("p1", "u1", List.of("BAD_ROLE"), "g"));
        }
    }
}
