package com.cloudys.project.entity;

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
@Table(name = "manage_milestones")
@Getter
@Setter
@NoArgsConstructor
public class Milestone {

    @Id
    @Column(name = "milestone_id")
    private String milestoneId;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Column(nullable = false)
    private String name;

    private String description;

    private String message;

    @Column(name = "milestone_type", nullable = false)
    private String milestoneType = "regular";

    @Column(name = "is_baseline", nullable = false)
    private Boolean isBaseline = false;

    private String sprint;

    private String version;

    @JdbcTypeCode(SqlTypes.JSON)
    private String tags;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @JdbcTypeCode(SqlTypes.JSON)
    private String metadata;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (milestoneType == null) milestoneType = "regular";
        if (isBaseline == null) isBaseline = false;
    }
}
