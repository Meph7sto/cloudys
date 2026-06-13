package com.cloudys.requirement.entity;

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
@Table(name = "manage_requirements")
@Getter
@Setter
@NoArgsConstructor
public class ManageRequirement {

    @Id
    @Column(name = "req_id")
    private String reqId;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Column(name = "requirement_type", nullable = false)
    private String requirementType = "top_level";

    @Column(nullable = false)
    private String status = "draft";

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String priority;

    private String assignee;

    @JdbcTypeCode(SqlTypes.JSON)
    private String tags;

    @Column(name = "due_date")
    private String dueDate;

    @Column(name = "parent_id")
    private String parentId;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 0;

    @Column(name = "source_req_id")
    private String sourceReqId;

    @Column(name = "source_level")
    private String sourceLevel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_fields")
    private String customFields;

    @Column(name = "priority_suggested")
    private Integer prioritySuggested;

    @Column(name = "priority_final")
    private Integer priorityFinal;

    @Column(name = "priority_decided_by")
    private String priorityDecidedBy;

    @Column(name = "priority_decided_at")
    private Instant priorityDecidedAt;

    @Column(name = "baseline_id")
    private Long baselineId;

    @Column(name = "is_planned", nullable = false)
    private Boolean isPlanned = false;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private Boolean deleted = false;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = "draft";
        if (orderIndex == null) orderIndex = 0;
        if (isPlanned == null) isPlanned = false;
        if (deleted == null) deleted = false;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
