package com.cloudys.requirementanalysis.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "requirements_l123")
@Getter
@Setter
@NoArgsConstructor
public class RequirementL123 {

    @Id
    @Column(name = "req_id")
    private String reqId;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(nullable = false)
    private String level;

    @Column(nullable = false)
    private String text;

    @Column(nullable = false)
    private String fingerprint;

    @Column(name = "anchor_span_id")
    private String anchorSpanId;

    @Column(name = "r")
    private Integer r;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
