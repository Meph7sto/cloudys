package com.cloudys.requirement.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotBlank;

public record CreateRequirementRequest(
        @NotBlank @JsonAlias("requirement_type") String requirementType,
        @NotBlank String title,
        String description,
        String status,
        @JsonAlias("parent_id") String parentId,
        @JsonAlias("order_index") Integer orderIndex,
        String priority,
        String assignee,
        List<String> tags,
        @JsonAlias("due_date") String dueDate,
        @JsonAlias("source_req_id") String sourceReqId,
        @JsonAlias("source_level") String sourceLevel,
        @JsonAlias("custom_fields") Map<String, Object> customFields
) {}
