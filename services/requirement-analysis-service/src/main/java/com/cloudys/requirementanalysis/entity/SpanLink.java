package com.cloudys.requirementanalysis.entity;

import java.time.Instant;

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
@Table(name = "span_links")
@Getter
@Setter
@NoArgsConstructor
public class SpanLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "context_run_id", nullable = false)
    private String contextRunId;

    @Column(name = "source_span_id", nullable = false)
    private String sourceSpanId;

    @Column(name = "target_span_id", nullable = false)
    private String targetSpanId;

    @Column(name = "relation_type", nullable = false)
    private String relationType;

    @Column(name = "strength", nullable = false)
    private Double strength;

    @Column(name = "note")
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
