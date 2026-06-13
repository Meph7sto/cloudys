package com.cloudys.requirementanalysis.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cloudys.common.core.exception.ErrorResponse;
import com.cloudys.requirementanalysis.client.InferenceServiceClient;
import com.cloudys.requirementanalysis.entity.ClassificationAnalysis;
import com.cloudys.requirementanalysis.repository.ClassificationAnalysisRepository;

@Service
public class ClassificationService {

    private final InferenceServiceClient inferenceServiceClient;
    private final ClassificationAnalysisRepository classificationAnalysisRepository;
    private final JsonSupport jsonSupport;

    public ClassificationService(InferenceServiceClient inferenceServiceClient,
                                  ClassificationAnalysisRepository classificationAnalysisRepository,
                                  JsonSupport jsonSupport) {
        this.inferenceServiceClient = inferenceServiceClient;
        this.classificationAnalysisRepository = classificationAnalysisRepository;
        this.jsonSupport = jsonSupport;
    }

    @Transactional
    public Map<String, Object> classifyRequirements(String sessionId, List<String> requirements) {
        Map<String, Object> request = Map.of(
                "requirements", requirements,
                "max_length", 512
        );

        Map<String, Object> inferenceResult;
        try {
            inferenceResult = inferenceServiceClient.classifyTexts(request);
        } catch (ErrorResponse e) {
            throw e;
        } catch (Exception e) {
            inferenceResult = buildFallbackClassification(requirements);
        }

        ClassificationAnalysis analysis = new ClassificationAnalysis();
        analysis.setSessionId(sessionId);
        analysis.setRequirements(jsonSupport.toJson(requirements));
        analysis.setPredictions(jsonSupport.toJson(inferenceResult.get("predictions")));
        analysis.setLabelDistribution(jsonSupport.toJson(inferenceResult.get("label_distribution")));
        analysis.setTotal(requirements.size());
        analysis.setResultJson(jsonSupport.toJson(inferenceResult));
        analysis.setCreatedAt(Instant.now());
        classificationAnalysisRepository.save(analysis);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", analysis.getId());
        result.put("session_id", sessionId);
        result.put("total", analysis.getTotal());
        result.put("predictions", jsonSupport.toList(analysis.getPredictions()));
        result.put("label_distribution", jsonSupport.toMap(analysis.getLabelDistribution()));
        result.put("created_at", analysis.getCreatedAt());
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getClassificationResults(String sessionId) {
        return classificationAnalysisRepository.findTopBySessionIdOrderByCreatedAtDesc(sessionId)
                .map(analysis -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("id", analysis.getId());
                    result.put("session_id", analysis.getSessionId());
                    result.put("predictions", jsonSupport.toList(analysis.getPredictions()));
                    result.put("label_distribution", jsonSupport.toMap(analysis.getLabelDistribution()));
                    result.put("total", analysis.getTotal());
                    result.put("result_json", jsonSupport.toMap(analysis.getResultJson()));
                    result.put("created_at", analysis.getCreatedAt());
                    return result;
                })
                .orElseThrow(() -> new ErrorResponse("session 不存在分类结果: " + sessionId, 404));
    }

    private Map<String, Object> buildFallbackClassification(List<String> requirements) {
        List<String> predictions = new ArrayList<>();
        Map<String, Integer> distribution = new LinkedHashMap<>();

        for (String requirement : requirements) {
            String label = inferLabel(requirement);
            predictions.add(label);
            distribution.merge(label, 1, Integer::sum);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("predictions", predictions);
        result.put("label_distribution", distribution);
        result.put("total", requirements.size());
        result.put("source", "beta-fallback");
        return result;
    }

    private String inferLabel(String requirement) {
        String text = requirement == null ? "" : requirement.toLowerCase(Locale.ROOT);
        if (text.contains("性能") || text.contains("响应") || text.contains("latency")
                || text.contains("throughput") || text.contains("encrypt")
                || text.contains("安全") || text.contains("可用性")) {
            return "non-functional";
        }
        return "functional";
    }
}
