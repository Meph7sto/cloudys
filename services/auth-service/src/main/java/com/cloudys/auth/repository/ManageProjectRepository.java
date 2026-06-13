package com.cloudys.auth.repository;

import com.cloudys.auth.entity.ManageProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManageProjectRepository extends JpaRepository<ManageProject, String> {

    List<ManageProject> findByStatus(String status);

    List<ManageProject> findByProductIdIn(List<String> productIds);
}
