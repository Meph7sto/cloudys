package com.cloudys.requirementanalysis.dto;

public record AnalysisRunRequest(
        String sessionId,
        String projectId,
        String contextRunId,
        Boolean autoImport,
        String mappingMode) {
}
