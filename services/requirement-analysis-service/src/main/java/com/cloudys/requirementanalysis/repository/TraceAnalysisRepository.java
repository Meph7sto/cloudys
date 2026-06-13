package com.cloudys.requirementanalysis.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cloudys.requirementanalysis.entity.TraceAnalysis;

public interface TraceAnalysisRepository extends JpaRepository<TraceAnalysis, Long> {

    List<TraceAnalysis> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    List<TraceAnalysis> findBySessionIdAndAnalysisType(String sessionId, String analysisType);
}
