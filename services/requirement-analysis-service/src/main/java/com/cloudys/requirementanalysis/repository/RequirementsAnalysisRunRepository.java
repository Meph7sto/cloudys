package com.cloudys.requirementanalysis.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cloudys.requirementanalysis.entity.RequirementsAnalysisRun;

public interface RequirementsAnalysisRunRepository extends JpaRepository<RequirementsAnalysisRun, String> {

    List<RequirementsAnalysisRun> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    List<RequirementsAnalysisRun> findByProjectIdOrderByCreatedAtDesc(String projectId);

    List<RequirementsAnalysisRun> findBySessionIdAndProjectId(String sessionId, String projectId);
}
