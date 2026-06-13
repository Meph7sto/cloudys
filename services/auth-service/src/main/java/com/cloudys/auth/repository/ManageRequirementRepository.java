package com.cloudys.auth.repository;

import com.cloudys.auth.entity.ManageRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManageRequirementRepository extends JpaRepository<ManageRequirement, String> {

    List<ManageRequirement> findByProjectId(String projectId);
}
