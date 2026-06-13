package com.cloudys.requirementanalysis.entity;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "low_level_requirements")
@Getter
@Setter
@NoArgsConstructor
public class LowLevelRequirement {

    @Id
    @Column(name = "req_id")
    private String reqId;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "source_top_id")
    private String sourceTopId;

    @Column(name = "source_top_text")
    private String sourceTopText;

    @Column(nullable = false)
    private String text;

    private String component;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "acceptance_criteria")
    private String acceptanceCriteria;

    @Column(name = "test_method")
    private String testMethod;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "interfaces")
    private String interfaces;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data_contracts")
    private String dataContracts;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "error_handling")
    private String errorHandling;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "nfr")
    private String nfr;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "open_questions")
    private String openQuestions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_ids")
    private String evidenceIds;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "source")
    private String source;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "meta")
    private String meta;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
