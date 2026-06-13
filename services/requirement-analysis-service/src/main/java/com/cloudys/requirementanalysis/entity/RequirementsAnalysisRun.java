package com.cloudys.requirementanalysis.entity;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "requirements_analysis_runs")
@Getter
@Setter
@NoArgsConstructor
public class RequirementsAnalysisRun {

    @Id
    @Column(name = "analysis_run_id")
    private String analysisRunId;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "project_id")
    private String projectId;

    @Column(name = "context_run_id")
    private String contextRunId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "high_level_requirements", nullable = false)
    private String highLevelRequirements = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "low_level_requirements", nullable = false)
    private String lowLevelRequirements = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trace_result", nullable = false)
    private String traceResult = "{}";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conflict_result", nullable = false)
    private String conflictResult = "{}";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "classification_result", nullable = false)
    private String classificationResult = "{}";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "meta_json")
    private String metaJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
