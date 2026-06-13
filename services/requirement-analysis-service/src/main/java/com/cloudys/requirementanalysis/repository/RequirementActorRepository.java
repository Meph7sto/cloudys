package com.cloudys.requirementanalysis.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cloudys.requirementanalysis.entity.RequirementActor;

public interface RequirementActorRepository extends JpaRepository<RequirementActor, String> {

    List<RequirementActor> findByRequirementId(String requirementId);

    List<RequirementActor> findByStatus(String status);

    List<RequirementActor> findByRequirementIdAndActorType(String requirementId, String actorType);
}
