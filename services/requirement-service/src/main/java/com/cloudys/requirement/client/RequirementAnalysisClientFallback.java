package com.cloudys.requirement.client;

import org.springframework.stereotype.Component;

import com.cloudys.common.core.exception.ErrorResponse;
import com.cloudys.requirement.dto.AnalysisImportResponse;

@Component
public class RequirementAnalysisClientFallback implements RequirementAnalysisClient {

    @Override
    public AnalysisImportResponse exportSessionRequirements(String sessionId) {
        throw new ErrorResponse("需求分析服务不可用，无法导入 session 需求", 503);
    }
}
