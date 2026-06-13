package com.cloudys.requirementanalysis.entity;

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
@Table(name = "requirement_actors")
@Getter
@Setter
@NoArgsConstructor
public class RequirementActor {

    @Id
    @Column(name = "actor_id")
    private String actorId;

    @Column(name = "requirement_id", nullable = false)
    private String requirementId;

    @Column(name = "actor_type", nullable = false)
    private String actorType;

    @Column(name = "actor_name", nullable = false)
    private String actorName;

    @Column(name = "status", nullable = false)
    private String status = "idle";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_json")
    private String configJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
