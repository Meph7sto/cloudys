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
@Table(name = "context_runs")
@Getter
@Setter
@NoArgsConstructor
public class ContextRun {

    @Id
    @Column(name = "context_run_id")
    private String contextRunId;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options_snapshot")
    private String optionsSnapshot;

    @Column(name = "status", nullable = false)
    private String status = "RUNNING";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "stats")
    private String stats;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
