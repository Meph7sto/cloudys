-- ========================
-- V3: Backfill auth schema when V1 was skipped by an earlier Flyway baseline
-- ========================

CREATE TABLE IF NOT EXISTS auth_users (
    user_id TEXT PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    display_name TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'member'
        CHECK (role IN ('super_admin', 'admin', 'member', 'viewer')),
    external_type TEXT CHECK (external_type IS NULL OR external_type IN ('CLIENT', 'CONTRACTOR')),
    avatar_url TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    registration_status TEXT CHECK (registration_status IN ('pending', 'approved', 'rejected')),
    rejection_reason TEXT,
    rejected_at TIMESTAMPTZ,
    rejected_by TEXT,
    approved_at TIMESTAMPTZ,
    approved_by TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_auth_users_username ON auth_users(username);
CREATE INDEX IF NOT EXISTS idx_auth_users_role ON auth_users(role);
CREATE INDEX IF NOT EXISTS idx_auth_users_registration_status ON auth_users(registration_status);

INSERT INTO auth_users (user_id, username, password_hash, display_name, role, is_active, registration_status, approved_at, approved_by)
VALUES ('test-user-001', 'test', '$2b$12$6m.Xy3YEOvHNSXvq79T/meEpiTFFOOYVaR2aEp8pbFlfqsr/XkLki', '测试用户', 'super_admin', TRUE, 'approved', NOW(), 'test-user-001')
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO auth_users (user_id, username, password_hash, display_name, role, is_active, registration_status, approved_at, approved_by)
VALUES ('super-admin-001', 'admin', '$2b$12$JaZlrZRNkzxUa3yWzGsL.OqgApgiD60JvaeChZVz2M5blTMH40Aly', '系统超级管理员', 'super_admin', TRUE, 'approved', NOW(), 'super-admin-001')
ON CONFLICT (user_id) DO NOTHING;
