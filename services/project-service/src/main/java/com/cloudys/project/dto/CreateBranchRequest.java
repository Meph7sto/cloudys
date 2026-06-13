package com.cloudys.project.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotBlank;

public record CreateBranchRequest(
        @JsonAlias("base_milestone_id") @NotBlank String baseMilestoneId,
        @NotBlank String name,
        Map<String, Object> metadata
) {}
