package com.cloudys.inference.dto.classification;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 需求分类请求（对应 Python ClassificationRequest）。
 */
public record ClassificationRequest(
        @NotEmpty List<String> requirements,
        @JsonProperty("batch_size") Integer batchSize,
        @JsonProperty("max_length") Integer maxLength
) {
    public ClassificationRequest {
        if (batchSize == null || batchSize <= 0) batchSize = 32;
        if (maxLength == null || maxLength <= 0) maxLength = 512;
    }
}
