package com.cloudys.requirementanalysis.dto;

import java.util.List;

public record ClassifyRequest(
        String sessionId,
        List<String> requirements) {
}
