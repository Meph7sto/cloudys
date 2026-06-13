package com.cloudys.requirement.entity;

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
@Table(name = "manage_defects")
@Getter
@Setter
@NoArgsConstructor
public class Defect {

    @Id
    @Column(name = "defect_id")
    private String defectId;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Column(name = "requirement_id", nullable = false)
    private String requirementId;

    @Column(nullable = false)
    private String title;

    @Column(name = "reproduce_steps", nullable = false)
    private String reproduceSteps = "";

    @Column(nullable = false)
    private String severity = "medium";

    @Column(nullable = false)
    private String priority = "P2";

    @Column(nullable = false)
    private String status = "open";

    private String reporter;

    @Column(name = "dev_assignee")
    private String devAssignee;

    @Column(name = "tester_assignee")
    private String testerAssignee;

    @Column(name = "current_assignee")
    private String currentAssignee;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (severity == null) severity = "medium";
        if (priority == null) priority = "P2";
        if (status == null) status = "open";
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
