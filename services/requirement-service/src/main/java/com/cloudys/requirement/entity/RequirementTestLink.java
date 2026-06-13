package com.cloudys.requirement.entity;

import java.time.Instant;

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
@Table(name = "manage_requirement_test_links")
@Getter
@Setter
@NoArgsConstructor
public class RequirementTestLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "link_id")
    private Long linkId;

    @Column(name = "requirement_id", nullable = false)
    private String requirementId;

    @Column(name = "test_case_id", nullable = false)
    private String testCaseId;

    @Column(name = "link_type", nullable = false)
    private String linkType = "verification";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (linkType == null || linkType.isBlank()) {
            linkType = "verification";
        }
    }
}
