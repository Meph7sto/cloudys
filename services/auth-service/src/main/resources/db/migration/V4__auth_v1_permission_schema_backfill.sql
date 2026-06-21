-- ========================
-- V4: Backfill permission tables when V1 was skipped by an earlier Flyway baseline
-- ========================

CREATE TABLE IF NOT EXISTS project_members (
    id BIGSERIAL PRIMARY KEY,
    project_id TEXT NOT NULL,
    user_id TEXT NOT NULL REFERENCES auth_users(user_id) ON DELETE CASCADE,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(project_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_project_members_project ON project_members(project_id);
CREATE INDEX IF NOT EXISTS idx_project_members_user ON project_members(user_id);

CREATE TABLE IF NOT EXISTS member_roles (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES project_members(id) ON DELETE CASCADE,
    role_id TEXT NOT NULL CHECK (role_id IN ('PO', 'BA', 'DEV', 'REVIEWER', 'QA', 'CONTRACTOR', 'CLIENT')),
    granted_by TEXT NOT NULL REFERENCES auth_users(user_id),
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(member_id, role_id)
);

CREATE INDEX IF NOT EXISTS idx_member_roles_member ON member_roles(member_id);
CREATE INDEX IF NOT EXISTS idx_member_roles_role ON member_roles(role_id);

CREATE TABLE IF NOT EXISTS review_assignments (
    id BIGSERIAL PRIMARY KEY,
    requirement_id TEXT NOT NULL,
    reviewer_id TEXT NOT NULL REFERENCES auth_users(user_id) ON DELETE CASCADE,
    initiated_by TEXT NOT NULL REFERENCES auth_users(user_id),
    seq SMALLINT NOT NULL DEFAULT 1,
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'WITHDRAWN')),
    comment TEXT,
    decided_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_review_assignments_req ON review_assignments(requirement_id);
CREATE INDEX IF NOT EXISTS idx_review_assignments_reviewer ON review_assignments(reviewer_id);
CREATE INDEX IF NOT EXISTS idx_review_assignments_status ON review_assignments(status);

CREATE TABLE IF NOT EXISTS baselines (
    id BIGSERIAL PRIMARY KEY,
    project_id TEXT NOT NULL,
    version TEXT NOT NULL CHECK (char_length(version) >= 1),
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    created_by TEXT NOT NULL REFERENCES auth_users(user_id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_baselines_project ON baselines(project_id);
CREATE INDEX IF NOT EXISTS idx_baselines_version ON baselines(project_id, version);

CREATE TABLE IF NOT EXISTS change_requests (
    id BIGSERIAL PRIMARY KEY,
    requirement_id TEXT NOT NULL,
    baseline_id BIGINT NOT NULL REFERENCES baselines(id) ON DELETE RESTRICT,
    requested_by TEXT NOT NULL REFERENCES auth_users(user_id),
    reason TEXT NOT NULL CHECK (char_length(reason) >= 1),
    change_summary TEXT NOT NULL CHECK (char_length(change_summary) >= 1),
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    reviewed_by TEXT REFERENCES auth_users(user_id),
    review_comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_change_requests_req ON change_requests(requirement_id);
CREATE INDEX IF NOT EXISTS idx_change_requests_baseline ON change_requests(baseline_id);
CREATE INDEX IF NOT EXISTS idx_change_requests_status ON change_requests(status);

CREATE TABLE IF NOT EXISTS user_product_scopes (
    id BIGSERIAL PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES auth_users(user_id) ON DELETE CASCADE,
    product_id TEXT NOT NULL,
    can_edit BOOLEAN NOT NULL DEFAULT FALSE,
    granted_by TEXT NOT NULL REFERENCES auth_users(user_id),
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, product_id)
);

CREATE INDEX IF NOT EXISTS idx_user_product_scopes_user ON user_product_scopes(user_id);
CREATE INDEX IF NOT EXISTS idx_user_product_scopes_product ON user_product_scopes(product_id);

CREATE TABLE IF NOT EXISTS user_project_scopes (
    id BIGSERIAL PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES auth_users(user_id) ON DELETE CASCADE,
    project_id TEXT NOT NULL,
    can_edit BOOLEAN NOT NULL DEFAULT FALSE,
    granted_by TEXT NOT NULL REFERENCES auth_users(user_id),
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, project_id)
);

CREATE INDEX IF NOT EXISTS idx_user_project_scopes_user ON user_project_scopes(user_id);
CREATE INDEX IF NOT EXISTS idx_user_project_scopes_project ON user_project_scopes(project_id);
