package com.cloudys.requirementanalysis.dto;

import java.util.List;

public record DedupRequest(
        String sessionId,
        List<String> requirementTexts) {
}
