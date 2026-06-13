package com.cloudys.requirement.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record UpdateDefectRequest(
        String title,
        @JsonAlias("reproduce_steps") String reproduceSteps,
        @JsonAlias("requirement_id") String requirementId,
        String severity,
        String priority,
        String status,
        String reporter,
        @JsonAlias("dev_assignee") String devAssignee,
        @JsonAlias("tester_assignee") String testerAssignee,
        @JsonAlias("current_assignee") String currentAssignee
) {}
