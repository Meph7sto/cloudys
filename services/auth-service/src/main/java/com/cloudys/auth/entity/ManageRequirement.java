package com.cloudys.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.Immutable;

/**
 * Read-only cross-service entity for manage_requirements table.
 * Owned by requirement-service; auth-service only queries for reference.
 */
@Entity
@Table(name = "manage_requirements")
@Immutable
@Getter
@Setter
public class ManageRequirement {

    @Id
    @Column(name = "req_id")
    private String requirementId;

    @Column(name = "project_id")
    private String projectId;

    @Column(name = "status")
    private String status;
}
