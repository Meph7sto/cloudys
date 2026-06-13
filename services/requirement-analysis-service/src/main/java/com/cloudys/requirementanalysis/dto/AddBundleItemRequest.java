package com.cloudys.requirementanalysis.dto;

public record AddBundleItemRequest(
        String spanId,
        String spanRef,
        Integer orderIndex) {
}
