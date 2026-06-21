-- ========================
-- V2: Backfill dashboard tables when V1 was skipped by an earlier shared-schema baseline
-- ========================

ALTER TABLE manage_milestones ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE manage_milestones ADD COLUMN IF NOT EXISTS message TEXT;
ALTER TABLE manage_milestones ADD COLUMN IF NOT EXISTS milestone_type TEXT NOT NULL DEFAULT 'regular';
ALTER TABLE manage_milestones ADD COLUMN IF NOT EXISTS is_baseline BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE manage_milestones ADD COLUMN IF NOT EXISTS sprint TEXT;
ALTER TABLE manage_milestones ADD COLUMN IF NOT EXISTS version TEXT;
ALTER TABLE manage_milestones ADD COLUMN IF NOT EXISTS tags JSONB;
ALTER TABLE manage_milestones ADD COLUMN IF NOT EXISTS metadata JSONB;
CREATE INDEX IF NOT EXISTS idx_manage_milestones_project ON manage_milestones(project_id);

ALTER TABLE manage_audit_logs ADD COLUMN IF NOT EXISTS product_id TEXT;
CREATE INDEX IF NOT EXISTS idx_manage_audit_logs_project ON manage_audit_logs(project_id);
CREATE INDEX IF NOT EXISTS idx_manage_audit_logs_product ON manage_audit_logs(product_id);

UPDATE manage_audit_logs
SET product_id = project_id,
    project_id = NULL
WHERE product_id IS NULL
  AND project_id IS NOT NULL
  AND (
    target_type IN ('product', 'product_member')
    OR target_type LIKE 'product%'
  );
