package com.cloudys.requirementanalysis.entity;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "conflict_analysis")
@Getter
@Setter
@NoArgsConstructor
public class ConflictAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id")
    private String batchId;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "requirement_a", nullable = false)
    private String requirementA;

    @Column(name = "requirement_b", nullable = false)
    private String requirementB;

    @Column(name = "is_conflict", nullable = false)
    private Boolean isConflict;

    @Column(name = "raw_response")
    private String rawResponse;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_json")
    private String resultJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
