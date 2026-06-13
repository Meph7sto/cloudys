package com.cloudys.requirementanalysis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bundle_items")
@Getter
@Setter
@NoArgsConstructor
public class BundleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bundle_id", nullable = false)
    private String bundleId;

    @Column(name = "span_id", nullable = false)
    private String spanId;

    @Column(name = "span_ref", nullable = false)
    private String spanRef;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;
}
