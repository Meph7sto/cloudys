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
@Table(name = "requirement_graph_relations")
@Getter
@Setter
@NoArgsConstructor
public class RequirementGraphRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_id", nullable = false)
    private String snapshotId;

    @Column(name = "run_id")
    private String runId;

    @Column(name = "snapshot_status", nullable = false)
    private String snapshotStatus = "final";

    @Column(name = "project_id")
    private String projectId;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "relation_mode", nullable = false)
    private String relationMode;

    @Column(name = "source_node_id", nullable = false)
    private String sourceNodeId;

    @Column(name = "target_node_id", nullable = false)
    private String targetNodeId;

    @Column(name = "source_req_id", nullable = false)
    private String sourceReqId;

    @Column(name = "target_req_id", nullable = false)
    private String targetReqId;

    @Column(name = "relation_type", nullable = false)
    private String relationType;

    @Column(name = "source_kind", nullable = false)
    private String sourceKind;

    @Column(name = "source_detail")
    private String sourceDetail;

    @Column(name = "weight", nullable = false)
    private Double weight = 0.0;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "reason")
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence")
    private String evidence;

    @Column(name = "model")
    private String model;

    @Column(name = "review_action")
    private String reviewAction;

    @Column(name = "review_package_id")
    private String reviewPackageId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "properties")
    private String properties;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "invalidated_at")
    private Instant invalidatedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "progress_json")
    private String progressJson;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
