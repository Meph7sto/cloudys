package com.cloudys.requirementanalysis.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cloudys.common.core.exception.ErrorResponse;
import com.cloudys.requirementanalysis.entity.LowLevelRequirement;
import com.cloudys.requirementanalysis.entity.LowLevelRequirementLink;
import com.cloudys.requirementanalysis.entity.RequirementL123;
import com.cloudys.requirementanalysis.repository.LowLevelRequirementLinkRepository;
import com.cloudys.requirementanalysis.repository.LowLevelRequirementRepository;
import com.cloudys.requirementanalysis.repository.RequirementL123Repository;

@Service
public class RequirementExportService {

    private final RequirementL123Repository requirementL123Repository;
    private final LowLevelRequirementRepository lowLevelRequirementRepository;
    private final LowLevelRequirementLinkRepository lowLevelRequirementLinkRepository;
    private final JsonSupport jsonSupport;

    public RequirementExportService(RequirementL123Repository requirementL123Repository,
                                    LowLevelRequirementRepository lowLevelRequirementRepository,
                                    LowLevelRequirementLinkRepository lowLevelRequirementLinkRepository,
                                    JsonSupport jsonSupport) {
        this.requirementL123Repository = requirementL123Repository;
        this.lowLevelRequirementRepository = lowLevelRequirementRepository;
        this.lowLevelRequirementLinkRepository = lowLevelRequirementLinkRepository;
        this.jsonSupport = jsonSupport;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> exportSessionRequirements(String sessionId) {
        List<RequirementL123> highLevelRequirements = requirementL123Repository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        List<LowLevelRequirement> lowLevelRequirements = lowLevelRequirementRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        List<LowLevelRequirementLink> links = lowLevelRequirementLinkRepository.findBySessionId(sessionId);
        if (highLevelRequirements.isEmpty() && lowLevelRequirements.isEmpty()) {
            throw new ErrorResponse("session 不存在或尚未生成需求", 404);
        }

        return Map.of(
                "session_id", sessionId,
                "requirements_l123", highLevelRequirements.stream().map(this::toHighLevelMap).toList(),
                "low_level_requirements", lowLevelRequirements.stream().map(this::toLowLevelMap).toList(),
                "low_level_requirement_links", links.stream().map(this::toLinkMap).toList()
        );
    }

    private Map<String, Object> toHighLevelMap(RequirementL123 item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("req_id", item.getReqId());
        map.put("session_id", item.getSessionId());
        map.put("level", item.getLevel());
        map.put("text", item.getText());
        map.put("fingerprint", item.getFingerprint());
        map.put("anchor_span_id", item.getAnchorSpanId());
        map.put("created_at", item.getCreatedAt());
        return map;
    }

    private Map<String, Object> toLowLevelMap(LowLevelRequirement item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("req_id", item.getReqId());
        map.put("session_id", item.getSessionId());
        map.put("source_top_id", item.getSourceTopId());
        map.put("source_top_text", item.getSourceTopText());
        map.put("text", item.getText());
        map.put("component", item.getComponent());
        map.put("acceptance_criteria", jsonSupport.toStringList(item.getAcceptanceCriteria()));
        map.put("test_method", item.getTestMethod());
        map.put("meta", jsonSupport.toMap(item.getMeta()));
        map.put("created_at", item.getCreatedAt());
        return map;
    }

    private Map<String, Object> toLinkMap(LowLevelRequirementLink item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("req_id", item.getReqId());
        map.put("session_id", item.getSessionId());
        map.put("top_req_id", item.getTopReqId());
        map.put("created_at", item.getCreatedAt());
        return map;
    }
}
