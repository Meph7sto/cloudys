package com.cloudys.requirementanalysis.dto;

public record CreateSpanLinkRequest(
        String contextRunId,
        String sourceSpanId,
        String targetSpanId,
        String relationType,
        Double strength,
        String note) {
}
