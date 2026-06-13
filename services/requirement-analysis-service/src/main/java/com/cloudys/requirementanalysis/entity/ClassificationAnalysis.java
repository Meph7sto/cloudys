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
@Table(name = "classification_analysis")
@Getter
@Setter
@NoArgsConstructor
public class ClassificationAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id")
    private String sessionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "requirements", nullable = false)
    private String requirements;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "predictions", nullable = false)
    private String predictions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "label_distribution")
    private String labelDistribution;

    @Column(name = "total")
    private Integer total;

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
