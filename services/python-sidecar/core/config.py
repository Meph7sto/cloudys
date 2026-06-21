import json
import os

from project_paths import ENV_FILE

# 尝试自动加载项目根目录的 .env，便于本地开发/测试
try:
    from dotenv import load_dotenv  # type: ignore
except ImportError:  # 若未安装则忽略，由 requirements 安装后生效
    load_dotenv = None

if load_dotenv:
    load_dotenv(ENV_FILE, override=False)


class Settings:
    BASE_DIR = str(ENV_FILE.parent / "services" / "python-sidecar")

    # 模型提供方 — 仅支持 deepseek
    MODEL_PROVIDER = "deepseek"
    CLASSIFICATION_PROVIDER = "deepseek"

    # DeepSeek API 配置
    DEEPSEEK_API_URL = os.getenv(
        "DEEPSEEK_API_URL",
        "https://api.deepseek.com",
    )
    DEEPSEEK_API_KEY = os.getenv("DEEPSEEK_API_KEY", "")
    DEEPSEEK_MODEL = os.getenv("DEEPSEEK_MODEL", "deepseek-v4-pro")

    # OpenAI / Compatibility config
    OPENAI_API_KEY = os.getenv("OPENAI_API_KEY") or DEEPSEEK_API_KEY
    OPENAI_BASE_URL = os.getenv("OPENAI_BASE_URL") or DEEPSEEK_API_URL

    # 是否跳过知识库（KB）索引加载（默认为 True，即跳过）
    SKIP_KB_LOAD = os.getenv("SKIP_KB_LOAD", "true").lower() == "true"

    # 需求分类标签配置
    CLASSIFICATION_LABELS = []
    CLASSIFICATION_OTHER_LABEL = os.getenv("CLASSIFICATION_OTHER_LABEL", "其他")

    _labels_raw = os.getenv("CLASSIFICATION_LABELS", "")
    if _labels_raw:
        try:
            parsed = json.loads(_labels_raw)
            if isinstance(parsed, list):
                CLASSIFICATION_LABELS = [
                    str(item).strip() for item in parsed if str(item).strip()
                ]
            else:
                CLASSIFICATION_LABELS = [
                    s.strip() for s in _labels_raw.split(",") if s.strip()
                ]
        except json.JSONDecodeError:
            CLASSIFICATION_LABELS = [
                s.strip() for s in _labels_raw.split(",") if s.strip()
            ]

    # 模型参数默认配置
    DEFAULT_MAX_TOKENS = 8096
    DEFAULT_SYSTEM_PROMPT = "你是一个有用的中文助手。"


# 实例化配置对象，供其他模块使用
settings = Settings()
