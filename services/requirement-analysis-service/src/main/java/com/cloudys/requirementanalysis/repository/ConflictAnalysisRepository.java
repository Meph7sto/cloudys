package com.cloudys.requirementanalysis.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cloudys.requirementanalysis.entity.ConflictAnalysis;

public interface ConflictAnalysisRepository extends JpaRepository<ConflictAnalysis, Long> {

    List<ConflictAnalysis> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    List<ConflictAnalysis> findByBatchId(String batchId);

    List<ConflictAnalysis> findBySessionIdAndIsConflictTrue(String sessionId);
}
