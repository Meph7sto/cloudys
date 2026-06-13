package com.cloudys.project.entity;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
@Table(name = "manage_branches")
@Getter
@Setter
@NoArgsConstructor
public class Branch {

    @Id
    @Column(name = "branch_id")
    private String branchId;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Column(name = "base_milestone_id", nullable = false)
    private String baseMilestoneId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String status = "active";

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    private String metadata;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = "active";
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
