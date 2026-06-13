package com.cloudys.auth.service;

import com.cloudys.auth.entity.MemberRole;
import com.cloudys.auth.entity.ProjectMember;
import com.cloudys.auth.repository.MemberRoleRepository;
import com.cloudys.auth.repository.ProjectMemberRepository;
import com.cloudys.common.core.exception.ErrorResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class MemberService {

    private final ProjectMemberRepository memberRepo;
    private final MemberRoleRepository roleRepo;
    private final UserService userService;

    private static final Set<String> VALID_PROJECT_ROLES = Set.of(
            "PO", "BA", "DEV", "REVIEWER", "QA", "CONTRACTOR", "CLIENT");

    public MemberService(ProjectMemberRepository memberRepo,
                         MemberRoleRepository roleRepo,
                         UserService userService) {
        this.memberRepo = memberRepo;
        this.roleRepo = roleRepo;
        this.userService = userService;
    }

    @Transactional
    public Map<String, Object> addMember(String projectId, String userId, List<String> roleIds, String grantedBy) {
        // Verify user exists
        userService.getById(userId);

        // Check if already a member
        if (memberRepo.existsByProjectIdAndUserId(projectId, userId)) {
            throw new ErrorResponse("用户已是项目成员", 400);
        }

        ProjectMember member = new ProjectMember();
        member.setProjectId(projectId);
        member.setUserId(userId);
        member = memberRepo.save(member);

        // Assign roles
        List<String> assignedRoles = new ArrayList<>();
        if (roleIds != null && !roleIds.isEmpty()) {
            for (String roleId : roleIds) {
                if (!VALID_PROJECT_ROLES.contains(roleId)) {
                    throw new ErrorResponse("无效的项目角色: " + roleId, 400);
                }
                MemberRole mr = new MemberRole();
                mr.setMemberId(member.getId());
                mr.setRoleId(roleId);
                mr.setGrantedBy(grantedBy);
                roleRepo.save(mr);
                assignedRoles.add(roleId);
            }
        }

        return toMemberMap(member, assignedRoles);
    }

    @Transactional
    public void removeMember(String projectId, String userId) {
        ProjectMember member = memberRepo.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ErrorResponse("成员不存在", 404));

        roleRepo.deleteAllByMemberId(member.getId());
        memberRepo.delete(member);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listMembers(String projectId) {
        List<ProjectMember> members = memberRepo.findByProjectId(projectId);

        return members.stream().map(m -> {
            List<String> roles = roleRepo.findRoleIdsByMemberId(m.getId());
            // Include user info
            try {
                var user = userService.getById(m.getUserId());
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", m.getId());
                map.put("project_id", m.getProjectId());
                map.put("user_id", m.getUserId());
                map.put("username", user.getUsername());
                map.put("display_name", user.getDisplayName());
                map.put("roles", roles);
                map.put("joined_at", m.getJoinedAt());
                return map;
            } catch (ErrorResponse e) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", m.getId());
                map.put("project_id", m.getProjectId());
                map.put("user_id", m.getUserId());
                map.put("roles", roles);
                map.put("joined_at", m.getJoinedAt());
                return map;
            }
        }).toList();
    }

    @Transactional
    public Map<String, Object> setUserRoles(String projectId, String userId,
                                             List<String> roleIds, String grantedBy) {
        ProjectMember member = memberRepo.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ErrorResponse("成员不存在", 404));

        // Validate all role IDs
        if (roleIds != null) {
            for (String roleId : roleIds) {
                if (!VALID_PROJECT_ROLES.contains(roleId)) {
                    throw new ErrorResponse("无效的项目角色: " + roleId, 400);
                }
            }
        }

        // Diff-based update: get current roles, compute add/remove
        List<String> currentRoles = roleRepo.findRoleIdsByMemberId(member.getId());
        Set<String> targetRoles = roleIds != null ? new HashSet<>(roleIds) : Set.of();

        // Remove roles not in target
        for (String current : currentRoles) {
            if (!targetRoles.contains(current)) {
                roleRepo.deleteByMemberAndRole(member.getId(), current);
            }
        }

        // Add new roles
        for (String target : targetRoles) {
            if (!currentRoles.contains(target)) {
                MemberRole mr = new MemberRole();
                mr.setMemberId(member.getId());
                mr.setRoleId(target);
                mr.setGrantedBy(grantedBy);
                roleRepo.save(mr);
            }
        }

        List<String> updatedRoles = roleRepo.findRoleIdsByMemberId(member.getId());
        return toMemberMap(member, updatedRoles);
    }

    @Transactional(readOnly = true)
    public List<String> getUserProjectIds(String userId) {
        return memberRepo.findProjectIdsByUserId(userId);
    }

    private Map<String, Object> toMemberMap(ProjectMember member, List<String> roles) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", member.getId());
        map.put("project_id", member.getProjectId());
        map.put("user_id", member.getUserId());
        map.put("roles", roles);
        map.put("joined_at", member.getJoinedAt());
        return map;
    }
}
