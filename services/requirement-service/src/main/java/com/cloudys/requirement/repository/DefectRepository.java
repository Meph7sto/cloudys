package com.cloudys.requirement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cloudys.requirement.entity.Defect;

public interface DefectRepository extends JpaRepository<Defect, String> {

    List<Defect> findByProjectIdOrderByCreatedAtDesc(String projectId);

    List<Defect> findByRequirementIdOrderByCreatedAtDesc(String requirementId);

    long countByRequirementIdAndStatusIn(String requirementId, List<String> statuses);
}
