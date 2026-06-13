package com.cloudys.inference.dto.l4;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

/**
 * L4 需求生成请求（对应 Python L4GenerateRequest）。
 */
public record GenerateL4Request(
        @NotEmpty List<TopRequirement> requirements,
        GenerateConfig config,
        String model,
        @JsonProperty("use_thinking_mode") Boolean useThinkingMode
) {
    public record TopRequirement(String id, String text) {}

    public record GenerateConfig(
            @JsonProperty("top_k_pattern") int topKPattern,
            @JsonProperty("top_k_spec") int topKSpec,
            @JsonProperty("top_k_nfr") int topKNfr,
            @JsonProperty("max_l4_per_top_req") int maxL4PerTopReq,
            @JsonProperty("min_l4_per_top_req") int minL4PerTopReq,
            @JsonProperty("confidence_threshold") double confidenceThreshold
    ) {
        public GenerateConfig {
            if (topKPattern <= 0) topKPattern = 3;
            if (topKSpec <= 0) topKSpec = 3;
            if (topKNfr <= 0) topKNfr = 3;
            if (maxL4PerTopReq <= 0) maxL4PerTopReq = 10;
            if (minL4PerTopReq < 0) minL4PerTopReq = 1;
            if (confidenceThreshold <= 0.0d || confidenceThreshold > 1.0d) confidenceThreshold = 0.6d;
        }
    }

    public GenerateL4Request {
        if (model == null || model.isBlank()) model = "deepseek-chat";
        if (useThinkingMode == null) useThinkingMode = true;
        if (config == null) config = new GenerateConfig(5, 3, 3, 10, 1, 0.6d);
    }
}
