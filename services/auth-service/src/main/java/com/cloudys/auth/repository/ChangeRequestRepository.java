package com.cloudys.auth.repository;

import com.cloudys.auth.entity.ChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, Long> {

    List<ChangeRequest> findByRequirementIdOrderByCreatedAtDesc(String requirementId);

    List<ChangeRequest> findByStatus(String status);

    List<ChangeRequest> findByBaselineId(Long baselineId);
}
