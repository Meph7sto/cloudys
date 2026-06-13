package com.cloudys.requirement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cloudys.requirement.entity.RequirementTestLink;

public interface RequirementTestLinkRepository extends JpaRepository<RequirementTestLink, Long> {

    List<RequirementTestLink> findByRequirementIdOrderByCreatedAtDesc(String requirementId);

    List<RequirementTestLink> findByTestCaseIdOrderByCreatedAtDesc(String testCaseId);

    Optional<RequirementTestLink> findByRequirementIdAndTestCaseId(String requirementId, String testCaseId);
}
