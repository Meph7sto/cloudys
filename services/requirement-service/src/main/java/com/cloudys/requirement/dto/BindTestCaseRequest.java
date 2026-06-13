package com.cloudys.requirement.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotBlank;

public record BindTestCaseRequest(
        @NotBlank @JsonAlias("test_case_id") String testCaseId,
        @JsonAlias("link_type") String linkType
) {}
