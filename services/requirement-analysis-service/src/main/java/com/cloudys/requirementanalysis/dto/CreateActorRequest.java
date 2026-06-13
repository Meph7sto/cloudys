package com.cloudys.requirementanalysis.dto;

import java.util.Map;

public record CreateActorRequest(
        String requirementId,
        String actorType,
        String actorName,
        String status,
        Map<String, Object> config) {
}
