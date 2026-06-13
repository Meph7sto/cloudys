package com.cloudys.auth.repository;

import com.cloudys.auth.entity.ReviewAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewAssignmentRepository extends JpaRepository<ReviewAssignment, Long> {

    List<ReviewAssignment> findByRequirementId(String requirementId);

    List<ReviewAssignment> findByReviewerIdAndStatus(String reviewerId, String status);

    Optional<ReviewAssignment> findByIdAndReviewerIdAndStatus(Long id, String reviewerId, String status);

    Optional<ReviewAssignment> findByIdAndInitiatedByAndStatus(Long id, String initiatedBy, String status);
}
