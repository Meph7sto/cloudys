package com.cloudys.requirementanalysis.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cloudys.requirementanalysis.entity.ClassificationAnalysis;

public interface ClassificationAnalysisRepository extends JpaRepository<ClassificationAnalysis, Long> {

    List<ClassificationAnalysis> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    Optional<ClassificationAnalysis> findTopBySessionIdOrderByCreatedAtDesc(String sessionId);
}
