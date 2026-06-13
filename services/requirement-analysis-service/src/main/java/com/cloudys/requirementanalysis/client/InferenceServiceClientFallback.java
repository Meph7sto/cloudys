package com.cloudys.requirementanalysis.client;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.cloudys.common.core.exception.ErrorResponse;

@Component
public class InferenceServiceClientFallback implements InferenceServiceClient {

    @Override
    public Map<String, Object> extractRequirements(Map<String, Object> request) {
        throw new ErrorResponse("推理服务不可用，无法提取需求", 503);
    }

    @Override
    public Map<String, Object> classifyTexts(Map<String, Object> request) {
        throw new ErrorResponse("推理服务不可用，无法分类需求", 503);
    }

    @Override
    public Map<String, Object> checkConflict(Map<String, Object> request) {
        throw new ErrorResponse("推理服务不可用，无法检测冲突", 503);
    }

    @Override
    public Map<String, Object> batchAnalyzeRelations(Map<String, Object> request) {
        throw new ErrorResponse("推理服务不可用，无法分析追溯关系", 503);
    }

    @Override
    public Map<String, Object> generateL4(Map<String, Object> request) {
        throw new ErrorResponse("推理服务不可用，无法生成L4需求", 503);
    }
}
