package com.cloudys.requirementanalysis.dto;

import java.util.Map;

public record CreateContextRunRequest(
        String sessionId,
        Map<String, Object> optionsSnapshot) {
}
