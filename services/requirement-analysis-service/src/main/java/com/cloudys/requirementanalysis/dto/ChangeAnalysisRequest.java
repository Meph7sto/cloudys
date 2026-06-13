package com.cloudys.requirementanalysis.dto;

import java.util.List;

public record ChangeAnalysisRequest(
        String sessionId,
        String projectId,
        List<String> changedReqIds) {
}
