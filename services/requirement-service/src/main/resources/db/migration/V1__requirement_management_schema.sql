-- ========================
-- V1: Requirement Management Schema
-- 需求管理服务拥有需求主数据、需求状态、测试关联与缺陷管理表。
-- ========================

CREATE TABLE IF NOT EXISTS manage_requirements (
    req_id TEXT PRIMARY KEY,
    project_id TEXT NOT NULL,
    requirement_type TEXT NOT NULL CHECK (requirement_type IN ('top_level', 'low_level', 'task')),
    status TEXT NOT NULL DEFAULT 'draft' CHECK (status IN ('draft', 'under_review', 'confirmed', 'in_progress', 'completed', 'archived')),
    title TEXT NOT NULL,
    description TEXT,
    priority TEXT,
    assignee TEXT,
    tags JSONB,
    due_date TEXT,
    parent_id TEXT REFERENCES manage_requirements(req_id) ON DELETE SET NULL,
    order_index INTEGER NOT NULL DEFAULT 0,
    source_req_id TEXT,
    source_level TEXT,
    custom_fields JSONB,
    priority_suggested INTEGER CHECK (priority_suggested IS NULL OR (priority_suggested >= 1 AND priority_suggested <= 5)),
    priority_final INTEGER CHECK (priority_final IS NULL OR (priority_final >= 1 AND priority_final <= 5)),
    priority_decided_by TEXT,
    priority_decided_at TIMESTAMPTZ,
    baseline_id BIGINT,
    is_planned BOOLEAN NOT NULL DEFAULT FALSE,
    created_by TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_manage_requirements_project ON manage_requirements(project_id);
CREATE INDEX IF NOT EXISTS idx_manage_requirements_parent ON manage_requirements(parent_id);
CREATE INDEX IF NOT EXISTS idx_manage_requirements_type ON manage_requirements(requirement_type);
CREATE INDEX IF NOT EXISTS idx_manage_requirements_status ON manage_requirements(status);
CREATE INDEX IF NOT EXISTS idx_manage_requirements_baseline ON manage_requirements(baseline_id);

CREATE TABLE IF NOT EXISTS manage_test_cases (
    test_case_id TEXT PRIMARY KEY,
    project_id TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    status TEXT NOT NULL DEFAULT 'draft' CHECK (status IN ('draft', 'active', 'deprecated')),
    source TEXT,
    created_by TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_manage_test_cases_project ON manage_test_cases(project_id);

CREATE TABLE IF NOT EXISTS manage_requirement_test_links (
    link_id SERIAL PRIMARY KEY,
    requirement_id TEXT NOT NULL REFERENCES manage_requirements(req_id) ON DELETE CASCADE,
    test_case_id TEXT NOT NULL REFERENCES manage_test_cases(test_case_id) ON DELETE CASCADE,
    link_type TEXT NOT NULL DEFAULT 'verification',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(requirement_id, test_case_id)
);

CREATE TABLE IF NOT EXISTS manage_defects (
    defect_id TEXT PRIMARY KEY,
    project_id TEXT NOT NULL,
    requirement_id TEXT NOT NULL REFERENCES manage_requirements(req_id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    reproduce_steps TEXT NOT NULL DEFAULT '',
    severity TEXT NOT NULL DEFAULT 'medium' CHECK (severity IN ('critical', 'high', 'medium', 'low')),
    priority TEXT NOT NULL DEFAULT 'P2' CHECK (priority IN ('P0', 'P1', 'P2', 'P3')),
    status TEXT NOT NULL DEFAULT 'open' CHECK (status IN ('open', 'in_progress', 'resolved', 'verified', 'closed', 'rejected')),
    reporter TEXT,
    dev_assignee TEXT,
    tester_assignee TEXT,
    current_assignee TEXT,
    created_by TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_manage_defects_project ON manage_defects(project_id);
CREATE INDEX IF NOT EXISTS idx_manage_defects_requirement ON manage_defects(requirement_id);
CREATE INDEX IF NOT EXISTS idx_manage_defects_status ON manage_defects(status);
