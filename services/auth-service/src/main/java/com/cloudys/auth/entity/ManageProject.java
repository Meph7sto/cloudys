package com.cloudys.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.Immutable;

/**
 * Read-only cross-service entity for manage_projects table.
 * Owned by project-service; auth-service only queries for scope options.
 */
@Entity
@Table(name = "manage_projects")
@Immutable
@Getter
@Setter
public class ManageProject {

    @Id
    @Column(name = "project_id")
    private String projectId;

    @Column(name = "name")
    private String projectName;

    @Column(name = "product_id")
    private String productId;

    @Column(name = "status")
    private String status;
}
