package com.cloudys.project.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cloudys.project.entity.Milestone;

@Repository
public interface MilestoneRepository extends JpaRepository<Milestone, String> {

    List<Milestone> findByProjectIdOrderByCreatedAtDesc(String projectId);
}
