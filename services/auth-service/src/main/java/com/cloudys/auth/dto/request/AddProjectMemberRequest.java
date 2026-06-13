package com.cloudys.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AddProjectMemberRequest(
        @JsonAlias("user_id")
        @NotBlank(message = "用户 ID 不能为空") String userId,
        @JsonAlias("member_roles")
        @NotEmpty(message = "成员角色不能为空") List<String> memberRoles
) {}
