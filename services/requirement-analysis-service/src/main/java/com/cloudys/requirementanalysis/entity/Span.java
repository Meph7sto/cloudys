package com.cloudys.requirementanalysis.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "spans")
@Getter
@Setter
@NoArgsConstructor
public class Span {

    @Id
    @Column(name = "span_id")
    private String spanId;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "start_ms")
    private Long startMs;

    @Column(name = "end_ms")
    private Long endMs;

    @Column(name = "speaker")
    private String speaker;

    @Column(name = "text", nullable = false)
    private String text;

    @Column(name = "asr_confidence")
    private Double asrConfidence;

    @Column(name = "meta_json")
    private String metaJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
