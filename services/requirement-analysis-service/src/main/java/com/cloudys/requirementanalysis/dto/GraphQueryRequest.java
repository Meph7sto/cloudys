package com.cloudys.requirementanalysis.dto;

public record GraphQueryRequest(
        String projectId,
        String sessionId,
        String relationMode,
        Boolean activeOnly) {
}
