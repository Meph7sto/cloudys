package com.cloudys.requirement.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAlias;

public record AnalysisImportResponse(
        @JsonAlias("requirements_l123") List<AnalysisRequirementRecord> requirementsL123,
        @JsonAlias("low_level_requirements") List<AnalysisLowLevelRequirementRecord> lowLevelRequirements,
        @JsonAlias("low_level_requirement_links") List<AnalysisLowLevelLinkRecord> lowLevelRequirementLinks
) {
    public List<AnalysisRequirementRecord> safeRequirementsL123() {
        return requirementsL123 != null ? requirementsL123 : List.of();
    }

    public List<AnalysisLowLevelRequirementRecord> safeLowLevelRequirements() {
        return lowLevelRequirements != null ? lowLevelRequirements : List.of();
    }

    public List<AnalysisLowLevelLinkRecord> safeLowLevelRequirementLinks() {
        return lowLevelRequirementLinks != null ? lowLevelRequirementLinks : List.of();
    }

    public record AnalysisRequirementRecord(
            @JsonAlias("req_id") String reqId,
            String level,
            String text
    ) {}

    public record AnalysisLowLevelRequirementRecord(
            @JsonAlias("req_id") String reqId,
            @JsonAlias("source_top_id") String sourceTopId,
            @JsonAlias("source_top_text") String sourceTopText,
            String text,
            String component,
            @JsonAlias("acceptance_criteria") List<String> acceptanceCriteria,
            @JsonAlias("test_method") String testMethod,
            Map<String, Object> meta
    ) {}

    public record AnalysisLowLevelLinkRecord(
            @JsonAlias("req_id") String reqId,
            @JsonAlias("top_req_id") String topReqId
    ) {}
}
