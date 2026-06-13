package com.cloudys.project.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cloudys.project.entity.Project;

@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {

    Optional<Project> findByName(String name);

    List<Project> findByStatus(String status);

    List<Project> findByProductId(String productId);
}
