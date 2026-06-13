package com.cloudys.requirementanalysis.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cloudys.requirementanalysis.entity.RequirementL123;

public interface RequirementL123Repository extends JpaRepository<RequirementL123, String> {

    List<RequirementL123> findBySessionIdOrderByCreatedAtAsc(String sessionId);
}
