package com.cloudys.auth.repository;

import com.cloudys.auth.entity.Baseline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BaselineRepository extends JpaRepository<Baseline, Long> {

    List<Baseline> findByProjectIdOrderByCreatedAtDesc(String projectId);

    List<Baseline> findByProjectIdAndLocked(String projectId, Boolean locked);

    Optional<Baseline> findTopByProjectIdAndVersion(String projectId, String version);

    boolean existsByProjectIdAndVersion(String projectId, String version);
}
