package com.cloudys.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;

public record UpdateProfileRequest(
        @JsonAlias("display_name")
        String displayName,
        @JsonAlias("avatar_url")
        String avatarUrl
) {}
