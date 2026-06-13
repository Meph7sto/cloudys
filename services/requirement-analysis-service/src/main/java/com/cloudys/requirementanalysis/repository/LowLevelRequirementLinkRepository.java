package com.cloudys.requirementanalysis.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cloudys.requirementanalysis.entity.LowLevelRequirementLink;
import com.cloudys.requirementanalysis.entity.LowLevelRequirementLinkId;

public interface LowLevelRequirementLinkRepository extends JpaRepository<LowLevelRequirementLink, LowLevelRequirementLinkId> {

    List<LowLevelRequirementLink> findBySessionId(String sessionId);
}
