package com.cloudys.project.repository;

import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cloudys.project.entity.Milestone;

@Repository
public interface MilestoneRepository extends JpaRepository<Milestone, String> {

    List<Milestone> findByProjectIdOrderByCreatedAtDesc(String projectId);

    @Query(value = """
            SELECT
                milestone_id,
                project_id,
                name,
                description,
                message,
                milestone_type,
                is_baseline,
                sprint,
                version,
                CAST(tags AS TEXT) AS tags,
                created_by,
                created_at,
                CAST(metadata AS TEXT) AS metadata
            FROM manage_milestones
            WHERE project_id = :projectId
            ORDER BY created_at DESC
            """, nativeQuery = true)
    List<Map<String, Object>> findDashboardRowsByProjectId(@Param("projectId") String projectId);
}
