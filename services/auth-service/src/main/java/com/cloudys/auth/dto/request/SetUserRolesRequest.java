package com.cloudys.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SetUserRolesRequest(
        @NotNull(message = "角色列表不能为 null") List<String> roles
) {}
