package com.cloudys.requirement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cloudys.requirement.entity.TestCase;

public interface TestCaseRepository extends JpaRepository<TestCase, String> {

    List<TestCase> findByProjectIdOrderByCreatedAtDesc(String projectId);

    boolean existsByProjectIdAndTestCaseId(String projectId, String testCaseId);
}
