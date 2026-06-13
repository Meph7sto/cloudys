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
@Table(name = "bundles")
@Getter
@Setter
@NoArgsConstructor
public class Bundle {

    @Id
    @Column(name = "bundle_id")
    private String bundleId;

    @Column(name = "context_run_id", nullable = false)
    private String contextRunId;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "status", nullable = false)
    private String status = "READY";

    @Column(name = "context_summary")
    private String contextSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "meta")
    private String meta;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
