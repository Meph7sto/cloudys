package com.cloudys.requirementanalysis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RequirementImportRequest(
        String sessionId,
        String mappingMode
) {
}
