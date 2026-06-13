-- ========================
-- V1: Project & Product Management Schema
-- 对应 Python project_schema.py
-- ========================

-- Products
CREATE TABLE IF NOT EXISTS manage_products (
    product_id TEXT PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    description TEXT,
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'archived')),
    roadmap TEXT,
    version TEXT,
    tags JSONB,
    created_by TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_manage_products_status ON manage_products(status);
CREATE INDEX IF NOT EXISTS idx_manage_products_created_at ON manage_products(created_at);

-- Product Members
CREATE TABLE IF NOT EXISTS manage_product_members (
    id SERIAL PRIMARY KEY,
    product_id TEXT NOT NULL REFERENCES manage_products(product_id) ON DELETE CASCADE,
    user_id TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'member' CHECK (role IN ('owner', 'admin', 'member', 'viewer')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(product_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_manage_product_members_product ON manage_product_members(product_id);
CREATE INDEX IF NOT EXISTS idx_manage_product_members_user ON manage_product_members(user_id);

-- Projects
CREATE TABLE IF NOT EXISTS manage_projects (
    project_id TEXT PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    description TEXT,
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'archived')),
    product_id TEXT REFERENCES manage_products(product_id) ON DELETE SET NULL,
    current_session_id TEXT,
    created_by TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_manage_projects_product ON manage_projects(product_id);

-- Milestones
CREATE TABLE IF NOT EXISTS manage_milestones (
    milestone_id TEXT PRIMARY KEY,
    project_id TEXT NOT NULL REFERENCES manage_projects(project_id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    description TEXT,
    message TEXT,
    milestone_type TEXT NOT NULL DEFAULT 'regular' CHECK (milestone_type IN ('regular', 'baseline', 'branch', 'merge')),
    is_baseline BOOLEAN NOT NULL DEFAULT FALSE,
    sprint TEXT,
    version TEXT,
    tags JSONB,
    created_by TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB
);

CREATE INDEX IF NOT EXISTS idx_manage_milestones_project ON manage_milestones(project_id);

-- Milestone Nodes
CREATE TABLE IF NOT EXISTS manage_milestone_nodes (
    snapshot_id TEXT PRIMARY KEY,
    milestone_id TEXT NOT NULL REFERENCES manage_milestones(milestone_id) ON DELETE CASCADE,
    requirement_id TEXT NOT NULL,
    requirement_type TEXT,
    status TEXT,
    title TEXT,
    description TEXT,
    parent_id TEXT,
    order_index INTEGER,
    snapshot_data JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_manage_milestone_nodes_milestone ON manage_milestone_nodes(milestone_id);

-- Branches
CREATE TABLE IF NOT EXISTS manage_branches (
    branch_id TEXT PRIMARY KEY,
    project_id TEXT NOT NULL REFERENCES manage_projects(project_id) ON DELETE CASCADE,
    base_milestone_id TEXT NOT NULL REFERENCES manage_milestones(milestone_id) ON DELETE RESTRICT,
    name TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'under_review', 'merged', 'closed')),
    created_by TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB
);

CREATE INDEX IF NOT EXISTS idx_manage_branches_project ON manage_branches(project_id);

-- Branch Refs
CREATE TABLE IF NOT EXISTS manage_branch_refs (
    ref_id TEXT PRIMARY KEY,
    project_id TEXT NOT NULL REFERENCES manage_projects(project_id) ON DELETE CASCADE,
    ref_name TEXT NOT NULL,
    ref_type TEXT NOT NULL DEFAULT 'branch' CHECK (ref_type IN ('branch', 'tag')),
    milestone_id TEXT REFERENCES manage_milestones(milestone_id) ON DELETE SET NULL,
    created_by TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(project_id, ref_name)
);

CREATE INDEX IF NOT EXISTS idx_manage_branch_refs_project ON manage_branch_refs(project_id);

-- Change Sets
CREATE TABLE IF NOT EXISTS manage_change_sets (
    change_id TEXT PRIMARY KEY,
    branch_id TEXT NOT NULL REFERENCES manage_branches(branch_id) ON DELETE CASCADE,
    change_type TEXT NOT NULL CHECK (change_type IN ('added', 'modified', 'deleted', 'moved')),
    requirement_id TEXT,
    before_data JSONB,
    after_data JSONB,
    created_by TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_manage_change_sets_branch ON manage_change_sets(branch_id);

-- Impact Reports
CREATE TABLE IF NOT EXISTS manage_impact_reports (
    report_id TEXT PRIMARY KEY,
    branch_id TEXT NOT NULL REFERENCES manage_branches(branch_id) ON DELETE CASCADE,
    summary TEXT,
    detail_json JSONB,
    status TEXT NOT NULL DEFAULT 'pending',
    created_by TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_manage_impact_reports_branch ON manage_impact_reports(branch_id);

-- Merge Logs
CREATE TABLE IF NOT EXISTS manage_merge_logs (
    merge_id TEXT PRIMARY KEY,
    branch_id TEXT NOT NULL REFERENCES manage_branches(branch_id) ON DELETE CASCADE,
    target_milestone_id TEXT REFERENCES manage_milestones(milestone_id) ON DELETE SET NULL,
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'conflicts', 'merged', 'failed')),
    summary TEXT,
    created_by TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_manage_merge_logs_branch ON manage_merge_logs(branch_id);

-- Reviews
CREATE TABLE IF NOT EXISTS manage_reviews (
    review_id TEXT PRIMARY KEY,
    branch_id TEXT NOT NULL REFERENCES manage_branches(branch_id) ON DELETE CASCADE,
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected', 'changes_requested')),
    reviewer TEXT,
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_manage_reviews_branch ON manage_reviews(branch_id);

-- Audit Logs
CREATE TABLE IF NOT EXISTS manage_audit_logs (
    log_id TEXT PRIMARY KEY,
    project_id TEXT,
    product_id TEXT,
    actor TEXT,
    action TEXT,
    target_type TEXT,
    target_id TEXT,
    detail JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_manage_audit_logs_project ON manage_audit_logs(project_id);
CREATE INDEX IF NOT EXISTS idx_manage_audit_logs_product ON manage_audit_logs(product_id);
