package com.cloudys.requirementanalysis.client;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "inference-service", fallback = InferenceServiceClientFallback.class)
public interface InferenceServiceClient {

    @PostMapping("/api/v2/inference/acquisition/extract")
    Map<String, Object> extractRequirements(@RequestBody Map<String, Object> request);

    @PostMapping("/api/v2/inference/classification/predict-texts")
    Map<String, Object> classifyTexts(@RequestBody Map<String, Object> request);

    @PostMapping("/api/v2/inference/conflict/check")
    Map<String, Object> checkConflict(@RequestBody Map<String, Object> request);

    @PostMapping("/api/v2/inference/traceability/batch-relation")
    Map<String, Object> batchAnalyzeRelations(@RequestBody Map<String, Object> request);

    @PostMapping("/api/v2/inference/l4/generate")
    Map<String, Object> generateL4(@RequestBody Map<String, Object> request);
}
