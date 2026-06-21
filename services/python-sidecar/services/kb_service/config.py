import os

from project_paths import KB_INDEX_DIR

# ========================
# 配置常量
# ========================
DEFAULT_EMBEDDING_MODEL = os.getenv(
    "KB_EMBEDDING_MODEL",
    "sentence-transformers/all-MiniLM-L6-v2",
)
DEFAULT_PERSIST_DIR = str(KB_INDEX_DIR)
