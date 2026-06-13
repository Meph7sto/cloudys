package com.cloudys.requirementanalysis.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cloudys.requirementanalysis.entity.LowLevelRequirement;

public interface LowLevelRequirementRepository extends JpaRepository<LowLevelRequirement, String> {

    List<LowLevelRequirement> findBySessionIdOrderByCreatedAtAsc(String sessionId);
}
