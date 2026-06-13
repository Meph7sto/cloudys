-- ========================
-- V1: Requirement Analysis Schema
-- 需求分析服务拥有提取上下文、分析运行、分类、冲突、追溯图谱与 Actor 分析表。
-- ========================

CREATE TABLE IF NOT EXISTS spans (
    span_id TEXT PRIMARY KEY,
    session_id TEXT NOT NULL,
    start_ms INTEGER,
    end_ms INTEGER,
    speaker TEXT,
    text TEXT NOT NULL,
    asr_confidence REAL,
    meta_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_spans_session_id ON spans(session_id);

CREATE TABLE IF NOT EXISTS context_runs (
    context_run_id TEXT PRIMARY KEY,
    session_id TEXT NOT NULL,
    options_snapshot JSONB,
    status TEXT NOT NULL DEFAULT 'RUNNING',
    stats JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_context_runs_session_id ON context_runs(session_id);

CREATE TABLE IF NOT EXISTS span_links (
    id SERIAL PRIMARY KEY,
    context_run_id TEXT NOT NULL REFERENCES context_runs(context_run_id) ON DELETE CASCADE,
    source_span_id TEXT NOT NULL REFERENCES spans(span_id) ON DELETE CASCADE,
    target_span_id TEXT NOT NULL REFERENCES spans(span_id) ON DELETE CASCADE,
    relation_type TEXT NOT NULL CHECK (relation_type IN ('continuation', 'elaboration', 'dependency', 'same_topic', 'conflict')),
    strength REAL NOT NULL CHECK (strength >= 0 AND strength <= 1),
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(context_run_id, source_span_id, target_span_id, relation_type)
);

CREATE INDEX IF NOT EXISTS idx_span_links_context_run ON span_links(context_run_id);
CREATE INDEX IF NOT EXISTS idx_span_links_source ON span_links(source_span_id);
CREATE INDEX IF NOT EXISTS idx_span_links_target ON span_links(target_span_id);

CREATE TABLE IF NOT EXISTS bundles (
    bundle_id TEXT PRIMARY KEY,
    context_run_id TEXT NOT NULL REFERENCES context_runs(context_run_id) ON DELETE CASCADE,
    session_id TEXT NOT NULL,
    order_index INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT 'READY',
    context_summary TEXT,
    meta JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_bundles_context_run ON bundles(context_run_id);
CREATE INDEX IF NOT EXISTS idx_bundles_session_id ON bundles(session_id);

CREATE TABLE IF NOT EXISTS bundle_items (
    id SERIAL PRIMARY KEY,
    bundle_id TEXT NOT NULL REFERENCES bundles(bundle_id) ON DELETE CASCADE,
    span_id TEXT NOT NULL REFERENCES spans(span_id) ON DELETE CASCADE,
    span_ref TEXT NOT NULL,
    order_index INTEGER NOT NULL,
    UNIQUE(bundle_id, span_ref),
    UNIQUE(bundle_id, span_id)
);

CREATE INDEX IF NOT EXISTS idx_bundle_items_bundle ON bundle_items(bundle_id);
CREATE INDEX IF NOT EXISTS idx_bundle_items_span ON bundle_items(span_id);

CREATE TABLE IF NOT EXISTS requirements_l123 (
    req_id TEXT PRIMARY KEY,
    session_id TEXT NOT NULL,
    level TEXT NOT NULL CHECK (level IN ('L1', 'L2', 'L3')),
    text TEXT NOT NULL CHECK (char_length(text) >= 10 AND char_length(text) <= 500),
    fingerprint TEXT NOT NULL UNIQUE,
    anchor_span_id TEXT REFERENCES spans(span_id) ON DELETE CASCADE,
    r INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_requirements_l123_session ON requirements_l123(session_id);
CREATE INDEX IF NOT EXISTS idx_requirements_l123_level ON requirements_l123(level);
CREATE INDEX IF NOT EXISTS idx_requirements_l123_fingerprint ON requirements_l123(fingerprint);
CREATE INDEX IF NOT EXISTS idx_requirements_l123_anchor_span ON requirements_l123(anchor_span_id);

CREATE TABLE IF NOT EXISTS low_level_requirements (
    req_id TEXT PRIMARY KEY,
    session_id TEXT NOT NULL,
    source_top_id TEXT,
    source_top_text TEXT,
    text TEXT NOT NULL,
    component TEXT,
    acceptance_criteria JSONB,
    test_method TEXT,
    interfaces JSONB,
    data_contracts JSONB,
    error_handling JSONB,
    nfr JSONB,
    open_questions JSONB,
    evidence_ids JSONB,
    confidence REAL,
    source TEXT,
    meta JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_low_level_requirements_session ON low_level_requirements(session_id);
CREATE INDEX IF NOT EXISTS idx_low_level_requirements_source_top ON low_level_requirements(source_top_id);

CREATE TABLE IF NOT EXISTS low_level_requirement_links (
    req_id TEXT NOT NULL REFERENCES low_level_requirements(req_id) ON DELETE CASCADE,
    session_id TEXT NOT NULL,
    top_req_id TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (req_id, top_req_id)
);

CREATE INDEX IF NOT EXISTS idx_low_level_requirement_links_session ON low_level_requirement_links(session_id);
CREATE INDEX IF NOT EXISTS idx_low_level_requirement_links_top ON low_level_requirement_links(top_req_id);

CREATE TABLE IF NOT EXISTS conflict_analysis (
    id SERIAL PRIMARY KEY,
    batch_id TEXT,
    session_id TEXT,
    requirement_a TEXT NOT NULL,
    requirement_b TEXT NOT NULL,
    is_conflict BOOLEAN NOT NULL,
    raw_response TEXT,
    result_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_conflict_analysis_session ON conflict_analysis(session_id);
CREATE INDEX IF NOT EXISTS idx_conflict_analysis_batch ON conflict_analysis(batch_id);
CREATE INDEX IF NOT EXISTS idx_conflict_analysis_created_at ON conflict_analysis(created_at);

CREATE TABLE IF NOT EXISTS classification_analysis (
    id SERIAL PRIMARY KEY,
    session_id TEXT,
    requirements JSONB NOT NULL,
    predictions JSONB NOT NULL,
    label_distribution JSONB,
    total INTEGER,
    result_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_classification_analysis_session ON classification_analysis(session_id);
CREATE INDEX IF NOT EXISTS idx_classification_analysis_created_at ON classification_analysis(created_at);

CREATE TABLE IF NOT EXISTS requirements_analysis_runs (
    analysis_run_id TEXT PRIMARY KEY,
    session_id TEXT NOT NULL,
    project_id TEXT,
    context_run_id TEXT,
    high_level_requirements JSONB NOT NULL DEFAULT '[]'::jsonb,
    low_level_requirements JSONB NOT NULL DEFAULT '[]'::jsonb,
    trace_result JSONB NOT NULL DEFAULT '{}'::jsonb,
    conflict_result JSONB NOT NULL DEFAULT '{}'::jsonb,
    classification_result JSONB NOT NULL DEFAULT '{}'::jsonb,
    meta_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_requirements_analysis_runs_session ON requirements_analysis_runs(session_id);
CREATE INDEX IF NOT EXISTS idx_requirements_analysis_runs_created_at ON requirements_analysis_runs(created_at);

CREATE TABLE IF NOT EXISTS requirement_graph_relations (
    id BIGSERIAL PRIMARY KEY,
    snapshot_id TEXT NOT NULL,
    run_id TEXT,
    snapshot_status TEXT NOT NULL DEFAULT 'final',
    project_id TEXT,
    session_id TEXT,
    relation_mode TEXT NOT NULL,
    source_node_id TEXT NOT NULL,
    target_node_id TEXT NOT NULL,
    source_req_id TEXT NOT NULL,
    target_req_id TEXT NOT NULL,
    relation_type TEXT NOT NULL,
    source_kind TEXT NOT NULL,
    source_detail TEXT,
    weight REAL NOT NULL DEFAULT 0,
    confidence REAL,
    reason TEXT,
    evidence JSONB,
    model TEXT,
    review_action TEXT,
    review_package_id TEXT,
    properties JSONB,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    invalidated_at TIMESTAMPTZ,
    progress_json JSONB,
    UNIQUE (snapshot_id, source_node_id, target_node_id, relation_type)
);

CREATE INDEX IF NOT EXISTS idx_requirement_graph_relations_snapshot ON requirement_graph_relations(snapshot_id);
CREATE INDEX IF NOT EXISTS idx_requirement_graph_relations_lookup ON requirement_graph_relations(project_id, session_id, relation_mode, is_active);
CREATE INDEX IF NOT EXISTS idx_requirement_graph_relations_status ON requirement_graph_relations(project_id, session_id, relation_mode, snapshot_status, is_active);
CREATE INDEX IF NOT EXISTS idx_requirement_graph_relations_source_req ON requirement_graph_relations(source_req_id);
CREATE INDEX IF NOT EXISTS idx_requirement_graph_relations_target_req ON requirement_graph_relations(target_req_id);
CREATE INDEX IF NOT EXISTS idx_requirement_graph_relations_type ON requirement_graph_relations(relation_type);

CREATE TABLE IF NOT EXISTS trace_analysis (
    id SERIAL PRIMARY KEY,
    session_id TEXT,
    analysis_type TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    input_json TEXT NOT NULL,
    output_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_trace_created_at ON trace_analysis(created_at);
CREATE INDEX IF NOT EXISTS idx_trace_session_id ON trace_analysis(session_id);

CREATE TABLE IF NOT EXISTS requirement_actors (
    actor_id TEXT PRIMARY KEY,
    requirement_id TEXT NOT NULL,
    actor_type TEXT NOT NULL CHECK (actor_type IN ('human', 'agent', 'system')),
    actor_name TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'idle' CHECK (status IN ('idle', 'working', 'blocked', 'done')),
    config_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_requirement_actors_requirement ON requirement_actors(requirement_id);
CREATE INDEX IF NOT EXISTS idx_requirement_actors_status ON requirement_actors(status);
