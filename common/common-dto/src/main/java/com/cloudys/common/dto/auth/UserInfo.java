package com.cloudys.common.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 用户信息，对应 Python get_current_user 返回的 user_info 字典。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserInfo(
        String userId,
        String username,
        String role,
        String externalType,
        String displayName
) {}
