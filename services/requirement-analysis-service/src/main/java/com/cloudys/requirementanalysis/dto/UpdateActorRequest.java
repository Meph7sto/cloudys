package com.cloudys.requirementanalysis.dto;

import java.util.Map;

public record UpdateActorRequest(
        String actorName,
        String actorType,
        String status,
        Map<String, Object> config) {
}
