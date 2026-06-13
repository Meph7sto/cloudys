package com.cloudys.project.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cloudys.project.entity.AuditLog;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    List<AuditLog> findByProjectIdOrderByCreatedAtDesc(String projectId, Pageable pageable);
}
