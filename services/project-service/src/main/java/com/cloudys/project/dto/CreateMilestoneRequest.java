package com.cloudys.project.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotBlank;

public record CreateMilestoneRequest(
        @NotBlank String name,
        String description,
        String message,
        @JsonAlias("milestone_type") String milestoneType,
        @JsonAlias("is_baseline") Boolean isBaseline,
        String sprint,
        String version,
        List<String> tags,
        Map<String, Object> metadata
) {}
