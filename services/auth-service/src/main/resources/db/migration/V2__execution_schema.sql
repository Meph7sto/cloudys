-- ========================
-- V2: Execution & Actor Schema (cross-service, managed by auth-service)
-- 对应 Python execution_schema.py + requirement_actor_schema.py
-- ========================

-- Execution Graphs
CREATE TABLE IF NOT EXISTS execution_graphs (
    graph_id TEXT PRIMARY KEY,
    project_id TEXT NOT NULL,
    name TEXT NOT NULL,
    description TEXT,
    graph_type TEXT NOT NULL CHECK (graph_type IN ('execution_topology_template', 'agent_runtime_execution')),
    version INTEGER NOT NULL DEFAULT 1,
    status TEXT NOT NULL DEFAULT 'draft' CHECK (status IN ('draft', 'active', 'running', 'succeeded', 'failed', 'suspended')),
    layout_json JSONB,
    meta_json JSONB,
    created_by TEXT,
    updated_by TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_execution_graphs_project ON execution_graphs(project_id);
CREATE INDEX IF NOT EXISTS idx_execution_graphs_status ON execution_graphs(status);
CREATE INDEX IF NOT EXISTS idx_execution_graphs_type ON execution_graphs(graph_type);

-- Execution Containers
CREATE TABLE IF NOT EXISTS execution_containers (
    container_id TEXT PRIMARY KEY,
    graph_id TEXT NOT NULL REFERENCES execution_graphs(graph_id) ON DELETE CASCADE,
    container_key TEXT NOT NULL,
    label TEXT NOT NULL,
    role_type TEXT NOT NULL CHECK (role_type IN ('human', 'agent', 'system')),
    config_json JSONB,
    order_index INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE(graph_id, container_key)
);

CREATE INDEX IF NOT EXISTS idx_execution_containers_graph ON execution_containers(graph_id);

-- Execution Nodes
CREATE TABLE IF NOT EXISTS execution_nodes (
    node_id TEXT PRIMARY KEY,
    graph_id TEXT NOT NULL REFERENCES execution_graphs(graph_id) ON DELETE CASCADE,
    container_id TEXT REFERENCES execution_containers(container_id) ON DELETE SET NULL,
    subgraph_id TEXT,
    node_key TEXT NOT NULL,
    label TEXT NOT NULL,
    node_type TEXT NOT NULL CHECK (node_type IN ('start', 'llm', 'handoff', 'invoke', 'end')),
    role_type TEXT NOT NULL CHECK (role_type IN ('human', 'agent', 'system')),
    phase_label TEXT,
    task_prompt TEXT,
    conditional_edges JSONB,
    config_json JSONB,
    input_schema_json JSONB,
    output_schema_json JSONB,
    order_index INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE(graph_id, node_key)
);

CREATE INDEX IF NOT EXISTS idx_execution_nodes_graph ON execution_nodes(graph_id);
CREATE INDEX IF NOT EXISTS idx_execution_nodes_container ON execution_nodes(container_id);
CREATE INDEX IF NOT EXISTS idx_execution_nodes_subgraph ON execution_nodes(subgraph_id);
CREATE INDEX IF NOT EXISTS idx_execution_nodes_type ON execution_nodes(node_type);

-- Execution Edges
CREATE TABLE IF NOT EXISTS execution_edges (
    edge_id TEXT PRIMARY KEY,
    graph_id TEXT NOT NULL REFERENCES execution_graphs(graph_id) ON DELETE CASCADE,
    source_node_id TEXT NOT NULL REFERENCES execution_nodes(node_id) ON DELETE CASCADE,
    target_node_id TEXT NOT NULL REFERENCES execution_nodes(node_id) ON DELETE CASCADE,
    edge_type TEXT NOT NULL CHECK (edge_type IN ('seq', 'call', 'return', 'retry')),
    label TEXT,
    condition_expr TEXT,
    config_json JSONB,
    order_index INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE(graph_id, source_node_id, target_node_id, edge_type)
);

CREATE INDEX IF NOT EXISTS idx_execution_edges_graph ON execution_edges(graph_id);
CREATE INDEX IF NOT EXISTS idx_execution_edges_source ON execution_edges(source_node_id);
CREATE INDEX IF NOT EXISTS idx_execution_edges_target ON execution_edges(target_node_id);

-- Execution Subgraphs
CREATE TABLE IF NOT EXISTS execution_subgraphs (
    subgraph_id TEXT PRIMARY KEY,
    graph_id TEXT NOT NULL REFERENCES execution_graphs(graph_id) ON DELETE CASCADE,
    container_id TEXT NOT NULL REFERENCES execution_containers(container_id) ON DELETE CASCADE,
    subgraph_key TEXT NOT NULL,
    label TEXT NOT NULL,
    task_description TEXT NOT NULL DEFAULT '',
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'running', 'completed', 'failed')),
    order_index INTEGER NOT NULL DEFAULT 0,
    config_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE(graph_id, subgraph_key)
);

CREATE INDEX IF NOT EXISTS idx_execution_subgraphs_graph ON execution_subgraphs(graph_id);
CREATE INDEX IF NOT EXISTS idx_execution_subgraphs_container ON execution_subgraphs(container_id);
CREATE INDEX IF NOT EXISTS idx_execution_subgraphs_status ON execution_subgraphs(status);

-- Agent Runtime Registry
CREATE TABLE IF NOT EXISTS agent_runtime_registry (
    runtime_id TEXT PRIMARY KEY,
    container_key TEXT,
    agent_type TEXT,
    runtime_profile TEXT NOT NULL DEFAULT 'default',
    implementation_type TEXT NOT NULL DEFAULT 'local',
    endpoint TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    capability_policy_json JSONB,
    quota_policy_json JSONB,
    memory_policy_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_agent_runtime_registry_lookup ON agent_runtime_registry(container_key, agent_type, runtime_profile);

-- Agent Runs
CREATE TABLE IF NOT EXISTS agent_runs (
    run_id TEXT PRIMARY KEY,
    graph_id TEXT NOT NULL REFERENCES execution_graphs(graph_id) ON DELETE CASCADE,
    subgraph_id TEXT REFERENCES execution_subgraphs(subgraph_id) ON DELETE SET NULL,
    container_key TEXT NOT NULL,
    agent_type TEXT NOT NULL,
    runtime_id TEXT NOT NULL,
    parent_run_id TEXT REFERENCES agent_runs(run_id) ON DELETE SET NULL,
    status TEXT NOT NULL CHECK (status IN ('queued', 'running', 'waiting_user', 'succeeded', 'failed', 'timed_out', 'cancelled')),
    input_ref JSONB,
    output_ref JSONB,
    usage_json JSONB,
    error_code TEXT,
    error_message TEXT,
    resume_token TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_agent_runs_graph ON agent_runs(graph_id);
CREATE INDEX IF NOT EXISTS idx_agent_runs_status ON agent_runs(status);

-- Container State Store
CREATE TABLE IF NOT EXISTS container_state_store (
    graph_id TEXT NOT NULL REFERENCES execution_graphs(graph_id) ON DELETE CASCADE,
    container_key TEXT NOT NULL,
    state_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    updated_by TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (graph_id, container_key)
);

-- Requirement Actors
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
