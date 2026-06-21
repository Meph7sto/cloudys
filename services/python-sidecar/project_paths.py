from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parent
REPO_ROOT = PROJECT_ROOT.parents[1]
ENV_FILE = REPO_ROOT / ".env"
DATA_DIR = PROJECT_ROOT / "data"
KB_INDEX_DIR = DATA_DIR / "kb_indexes"
AUDIT_LOG_DIR = DATA_DIR / "audit_logs"
