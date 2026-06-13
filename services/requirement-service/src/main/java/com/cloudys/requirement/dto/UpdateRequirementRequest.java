package com.cloudys.requirement.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAlias;

public record UpdateRequirementRequest(
        String title,
        String description,
        String status,
        String priority,
        String assignee,
        List<String> tags,
        @JsonAlias("due_date") String dueDate,
        @JsonAlias("custom_fields") Map<String, Object> customFields,
        @JsonAlias("order_index") Integer orderIndex
) {}
