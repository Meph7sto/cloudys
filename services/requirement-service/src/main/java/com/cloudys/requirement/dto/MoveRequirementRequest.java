package com.cloudys.requirement.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record MoveRequirementRequest(
        @JsonAlias("parent_id") String parentId,
        @JsonAlias("order_index") Integer orderIndex
) {}
