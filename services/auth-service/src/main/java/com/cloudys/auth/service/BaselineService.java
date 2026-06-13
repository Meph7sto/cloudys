package com.cloudys.auth.service;

import com.cloudys.auth.entity.Baseline;
import com.cloudys.auth.repository.BaselineRepository;
import com.cloudys.common.core.exception.ErrorResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class BaselineService {

    private final BaselineRepository baselineRepo;

    public BaselineService(BaselineRepository baselineRepo) {
        this.baselineRepo = baselineRepo;
    }

    @Transactional
    public Map<String, Object> create(String projectId, String version, Boolean locked, String createdBy) {
        if (version == null || version.isBlank()) {
            throw new ErrorResponse("版本号不能为空", 400);
        }
        if (baselineRepo.existsByProjectIdAndVersion(projectId, version.trim())) {
            throw new ErrorResponse("该版本号已存在", 400);
        }

        Baseline baseline = new Baseline();
        baseline.setProjectId(projectId);
        baseline.setVersion(version.trim());
        baseline.setLocked(locked != null ? locked : false);
        baseline.setCreatedBy(createdBy);
        baseline = baselineRepo.save(baseline);

        return toMap(baseline);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listBaselines(String projectId) {
        return baselineRepo.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::toMap).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getBaseline(Long baselineId) {
        Baseline baseline = baselineRepo.findById(baselineId)
                .orElseThrow(() -> new ErrorResponse("基线不存在", 404));
        return toMap(baseline);
    }

    @Transactional
    public Map<String, Object> lockBaseline(Long baselineId) {
        Baseline baseline = baselineRepo.findById(baselineId)
                .orElseThrow(() -> new ErrorResponse("基线不存在", 404));
        if (baseline.getLocked()) {
            throw new ErrorResponse("基线已锁定", 400);
        }
        baseline.setLocked(true);
        return toMap(baselineRepo.save(baseline));
    }

    @Transactional
    public Map<String, Object> unlockBaseline(Long baselineId) {
        Baseline baseline = baselineRepo.findById(baselineId)
                .orElseThrow(() -> new ErrorResponse("基线不存在", 404));
        if (!baseline.getLocked()) {
            throw new ErrorResponse("基线未锁定", 400);
        }
        baseline.setLocked(false);
        return toMap(baselineRepo.save(baseline));
    }

    private Map<String, Object> toMap(Baseline b) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", b.getId());
        map.put("project_id", b.getProjectId());
        map.put("version", b.getVersion());
        map.put("locked", b.getLocked());
        map.put("created_by", b.getCreatedBy());
        map.put("created_at", b.getCreatedAt());
        return map;
    }
}
