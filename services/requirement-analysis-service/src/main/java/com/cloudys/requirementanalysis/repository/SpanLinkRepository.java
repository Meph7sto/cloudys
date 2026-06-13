package com.cloudys.requirementanalysis.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cloudys.requirementanalysis.entity.SpanLink;

public interface SpanLinkRepository extends JpaRepository<SpanLink, Long> {

    List<SpanLink> findByContextRunId(String contextRunId);

    List<SpanLink> findByContextRunIdAndRelationType(String contextRunId, String relationType);

    void deleteByContextRunId(String contextRunId);
}
