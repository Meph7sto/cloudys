package com.cloudys.requirementanalysis.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cloudys.requirementanalysis.entity.ContextRun;

public interface ContextRunRepository extends JpaRepository<ContextRun, String> {

    List<ContextRun> findBySessionIdOrderByCreatedAtDesc(String sessionId);
}
