package com.cloudys.requirementanalysis.dto;

import java.util.List;

public record InvalidateRelationsRequest(
        String snapshotId,
        List<Long> relationIds,
        String reason) {
}
