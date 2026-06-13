package com.cloudys.requirementanalysis.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cloudys.requirementanalysis.entity.Bundle;

public interface BundleRepository extends JpaRepository<Bundle, String> {

    List<Bundle> findByContextRunIdOrderByOrderIndexAsc(String contextRunId);

    List<Bundle> findBySessionIdOrderByOrderIndexAsc(String sessionId);
}
