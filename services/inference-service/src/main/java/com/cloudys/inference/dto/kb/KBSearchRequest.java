package com.cloudys.inference.dto.kb;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 知识库搜索请求（对应 Python KBSearchRequest）。
 */
public record KBSearchRequest(
        @NotBlank String query,
        @JsonProperty("kb_type") String kbType,
        @JsonProperty("top_k") int topK,
        Map<String, String> filters
) {
    public KBSearchRequest {
        if (kbType == null || kbType.isBlank()) kbType = "nfr";
        if (topK <= 0) topK = 5;
        if (filters == null) filters = Map.of();
    }
}
