CREATE TABLE spans (
    span_id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL
);

CREATE TABLE context_runs (
    context_run_id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL
);
