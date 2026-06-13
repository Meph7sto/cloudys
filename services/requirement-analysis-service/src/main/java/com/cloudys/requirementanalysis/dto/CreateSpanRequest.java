package com.cloudys.requirementanalysis.dto;

import java.util.Map;

public record CreateSpanRequest(
        String sessionId,
        Long startMs,
        Long endMs,
        String speaker,
        String text,
        Double asrConfidence,
        Map<String, Object> meta) {
}
