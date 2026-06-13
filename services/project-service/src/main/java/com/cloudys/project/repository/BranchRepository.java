package com.cloudys.project.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cloudys.project.entity.Branch;

@Repository
public interface BranchRepository extends JpaRepository<Branch, String> {

    List<Branch> findByProjectIdOrderByCreatedAtDesc(String projectId);
}
