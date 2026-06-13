package com.cloudys.requirementanalysis.dto;

import java.util.Map;

public record CreateBundleRequest(
        String contextRunId,
        String sessionId,
        Integer orderIndex,
        String contextSummary,
        Map<String, Object> meta) {
}
