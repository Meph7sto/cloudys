package com.cloudys.requirementanalysis.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cloudys.requirementanalysis.entity.Span;

public interface SpanRepository extends JpaRepository<Span, String> {

    List<Span> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    List<Span> findBySessionIdAndSpeaker(String sessionId, String speaker);

    void deleteBySessionId(String sessionId);
}
