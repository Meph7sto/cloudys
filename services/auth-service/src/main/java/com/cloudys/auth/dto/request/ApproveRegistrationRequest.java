package com.cloudys.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;

public record ApproveRegistrationRequest(
        String role,
        @JsonAlias("external_type")
        String externalType
) {}
