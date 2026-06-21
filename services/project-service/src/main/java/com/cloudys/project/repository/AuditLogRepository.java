package com.cloudys.project.repository;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cloudys.project.entity.AuditLog;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    List<AuditLog> findByProjectIdOrderByCreatedAtDesc(String projectId, Pageable pageable);

    @Query(value = """
            SELECT
                log_id,
                project_id,
                product_id,
                actor,
                action,
                target_type,
                target_id,
                CAST(detail AS TEXT) AS detail,
                created_at
            FROM manage_audit_logs
            WHERE project_id = :projectId
            ORDER BY created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Map<String, Object>> findDashboardRowsByProjectId(@Param("projectId") String projectId, @Param("limit") int limit);
}
