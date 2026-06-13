package com.cloudys.requirementanalysis.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "low_level_requirement_links")
@IdClass(LowLevelRequirementLinkId.class)
@Getter
@Setter
@NoArgsConstructor
public class LowLevelRequirementLink {

    @Id
    @Column(name = "req_id")
    private String reqId;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Id
    @Column(name = "top_req_id")
    private String topReqId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
