package com.cloudys.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.Immutable;

/**
 * Read-only cross-service entity for manage_products table.
 * Owned by project-service; auth-service only queries for scope options.
 */
@Entity
@Table(name = "manage_products")
@Immutable
@Getter
@Setter
public class ManageProduct {

    @Id
    @Column(name = "product_id")
    private String productId;

    @Column(name = "name")
    private String productName;

    @Column(name = "status")
    private String status;
}
