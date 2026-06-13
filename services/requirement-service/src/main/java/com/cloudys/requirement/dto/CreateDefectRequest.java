package com.cloudys.requirement.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotBlank;

public record CreateDefectRequest(
        @NotBlank @JsonAlias("requirement_id") String requirementId,
        @NotBlank String title,
        @NotBlank @JsonAlias("reproduce_steps") String reproduceSteps,
        String severity,
        String priority,
        String status,
        String reporter,
        @JsonAlias("dev_assignee") String devAssignee,
        @JsonAlias("tester_assignee") String testerAssignee,
        @JsonAlias("current_assignee") String currentAssignee
) {}
