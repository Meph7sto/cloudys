CREATE TABLE IF NOT EXISTS manage_milestones (
    milestone_id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS manage_audit_logs (
    log_id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64)
);
