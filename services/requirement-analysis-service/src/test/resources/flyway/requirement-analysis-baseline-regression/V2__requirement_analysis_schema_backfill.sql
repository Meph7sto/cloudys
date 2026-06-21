CREATE TABLE IF NOT EXISTS spans (
    span_id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL
);

CREATE TABLE IF NOT EXISTS context_runs (
    context_run_id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL
);
