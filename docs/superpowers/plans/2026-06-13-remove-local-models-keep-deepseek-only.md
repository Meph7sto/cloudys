# Remove Local Model Support — Keep DeepSeek API Only

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove all local model (HuggingFace), Chutes API, and knowledge-base embedding support from the inference backend, keeping only DeepSeek API as the sole inference provider.

**Architecture:** The Python inference sidecar (`Semantic-Atlas/backend_inference/`) currently supports three providers — local (HuggingFace), chutes (remote API), and deepseek (remote API). We will strip it down to deepseek-only: remove the `core/engine/local/` directory, delete Chutes clients, simplify the `DeepSeekEngine` to hardcode deepseek provider, remove local classification model loading from `classification_engine.py`, and clean up config/env/docker references. The Java inference service and frontend are thin pass-through layers that already default to `deepseek-v4-pro` — only minor cosmetic cleanup is needed there.

**Tech Stack:** Python 3.11 (FastAPI), Java 17 (Spring Boot), Vue 3 (Vite)

---

## File Map

| File | Action | Responsibility |
|------|--------|----------------|
| `Semantic-Atlas/backend_inference/core/engine/local/__init__.py` | **Delete** | LocalHFEngine re-export |
| `Semantic-Atlas/backend_inference/core/engine/local/hf_engine.py` | **Delete** | Local HuggingFace model loading + generation |
| `Semantic-Atlas/backend_inference/core/engine/local/dtype.py` | **Delete** | Torch dtype resolution for local models |
| `Semantic-Atlas/backend_inference/core/engine/remote/chutes.py` | **Delete** | Chutes sync API client |
| `Semantic-Atlas/backend_inference/core/engine/remote/async_chutes.py` | **Delete** | Chutes async API client |
| `Semantic-Atlas/backend_inference/core/engine/remote/sse.py` | **Keep** | SSE stream parsing (used by deepseek client) |
| `Semantic-Atlas/backend_inference/core/engine/remote/deepseek.py` | **Keep (no changes)** | DeepSeek sync API client |
| `Semantic-Atlas/backend_inference/core/engine/remote/async_deepseek.py` | **Keep (no changes)** | DeepSeek async API client |
| `Semantic-Atlas/backend_inference/core/engine/remote/__init__.py` | **Modify** | Remove ChutesClient export |
| `Semantic-Atlas/backend_inference/core/engine/__init__.py` | **Modify** | Simplify DeepSeekEngine — remove local/chutes branches, hardcode deepseek |
| `Semantic-Atlas/backend_inference/core/config.py` | **Modify** | Remove local fallback, chutes config, SKIP_MODEL_LOAD, local model paths |
| `Semantic-Atlas/backend_inference/core/model_container.py` | **Modify** | Remove chutes params from engine/classifier instantiation |
| `Semantic-Atlas/backend_inference/core/classification_engine.py` | **Modify** | Remove local HF model loading, keep only deepseek remote classification |
| `Semantic-Atlas/backend_inference/main.py` | **Modify** | Remove local model startup logic, `_should_skip_local_provider_load()` |
| `Semantic-Atlas/backend_inference/requirements.txt` | **Modify** | Remove torch, transformers, accelerate, sentence-transformers, faiss-cpu, llama-index |
| `cloudys/.env.example` | **Modify** | Remove SKIP_MODEL_LOAD, SKIP_KB_LOAD, chutes configs |
| `cloudys/docker-compose.yml` | **Modify** | Remove SKIP_MODEL_LOAD, SKIP_KB_LOAD env vars from python-sidecar |
| `cloudys/deploy/k8s/09-inference-service.yml` | **Modify** | Remove SKIP_MODEL_LOAD, SKIP_KB_LOAD env vars |
| `cloudys/frontend/src/views/beta/RequirementGraphView.vue` | **Modify** | Fix "本地模型" label, cleanup localVectorSummary naming |

---

### Task 1: Delete local HuggingFace engine files

**Files:**
- Delete: `Semantic-Atlas/backend_inference/core/engine/local/__init__.py`
- Delete: `Semantic-Atlas/backend_inference/core/engine/local/hf_engine.py`
- Delete: `Semantic-Atlas/backend_inference/core/engine/local/dtype.py`

- [ ] **Step 1: Delete the local engine directory files**

```bash
rm -rf "../Semantic-Atlas/backend_inference/core/engine/local/"
```

- [ ] **Step 2: Verify deletion**

Run: `ls "../Semantic-Atlas/backend_inference/core/engine/local/" 2>&1`
Expected: `No such file or directory`

- [ ] **Step 3: Commit**

```bash
git -C ../Semantic-Atlas add backend_inference/core/engine/local/
git -C ../Semantic-Atlas commit -m "refactor: remove local HuggingFace engine (LocalHFEngine, dtype)"
```

---

### Task 2: Delete Chutes API client files

**Files:**
- Delete: `Semantic-Atlas/backend_inference/core/engine/remote/chutes.py`
- Delete: `Semantic-Atlas/backend_inference/core/engine/remote/async_chutes.py`

- [ ] **Step 1: Delete the Chutes client files**

```bash
rm "../Semantic-Atlas/backend_inference/core/engine/remote/chutes.py"
rm "../Semantic-Atlas/backend_inference/core/engine/remote/async_chutes.py"
```

- [ ] **Step 2: Verify deletion**

Run: `ls "../Semantic-Atlas/backend_inference/core/engine/remote/chutes.py" "../Semantic-Atlas/backend_inference/core/engine/remote/async_chutes.py" 2>&1`
Expected: `No such file or directory` for both

- [ ] **Step 3: Commit**

```bash
git -C ../Semantic-Atlas add backend_inference/core/engine/remote/chutes.py backend_inference/core/engine/remote/async_chutes.py
git -C ../Semantic-Atlas commit -m "refactor: remove Chutes API sync and async clients"
```

---

### Task 3: Simplify remote engine __init__.py exports

**Files:**
- Modify: `Semantic-Atlas/backend_inference/core/engine/remote/__init__.py`

- [ ] **Step 1: Read current file to confirm content**

The file currently reads:
```python
from .chutes import ChutesClient
from .deepseek import DeepSeekClient, ToolCallResult

__all__ = ["ChutesClient", "DeepSeekClient", "ToolCallResult"]
```

- [ ] **Step 2: Replace with deepseek-only exports**

```python
from .deepseek import DeepSeekClient, ToolCallResult

__all__ = ["DeepSeekClient", "ToolCallResult"]
```

- [ ] **Step 3: Commit**

```bash
git -C ../Semantic-Atlas add backend_inference/core/engine/remote/__init__.py
git -C ../Semantic-Atlas commit -m "refactor: remove ChutesClient export from remote engine __init__"
```

---

### Task 4: Simplify DeepSeekEngine — remove local and chutes branches

**Files:**
- Modify: `Semantic-Atlas/backend_inference/core/engine/__init__.py`

- [ ] **Step 1: Write the simplified DeepSeekEngine**

Replace the entire file content. The new version hardcodes `provider="deepseek"`, removes all local model and chutes branches, removes `_get_local_engine()`, `_get_remote_client()` (since there's only one provider now), and removes `torch`/`weakref` imports.

```python
from typing import Any, AsyncGenerator, Dict, Generator, List, Optional

import asyncio

from ..config import settings
from .remote import DeepSeekClient, ToolCallResult
from .remote.deepseek import DEFAULT_DEEPSEEK_MODEL
from .remote.async_deepseek import AsyncDeepSeekClient


class DeepSeekEngine:
    """DeepSeek API 模型引擎 — 通过 DeepSeek API 执行文本生成任务。"""

    def __init__(
        self,
        deepseek_api_url: Optional[str] = None,
        deepseek_api_key: Optional[str] = None,
        deepseek_model: Optional[str] = None,
    ):
        self.deepseek_api_url = deepseek_api_url or settings.DEEPSEEK_API_URL
        self.deepseek_api_key = deepseek_api_key or settings.DEEPSEEK_API_KEY
        self.deepseek_model = deepseek_model or getattr(
            settings, "DEEPSEEK_MODEL", DEFAULT_DEEPSEEK_MODEL
        )
        self.model = None
        self.tokenizer = None
        self._sync_client = None
        self._async_client: Optional[AsyncDeepSeekClient] = None

    def _get_sync_client(self) -> DeepSeekClient:
        if self._sync_client is None:
            self._sync_client = DeepSeekClient(
                self.deepseek_api_url,
                self.deepseek_api_key,
                model=self.deepseek_model,
            )
        return self._sync_client

    async def _get_async_client(self) -> AsyncDeepSeekClient:
        if self._async_client is None:
            self._async_client = AsyncDeepSeekClient(
                self.deepseek_api_url,
                self.deepseek_api_key,
                model=self.deepseek_model,
            )
        await self._async_client.ensure_session()
        return self._async_client

    def load_model(self):
        """标记引擎为可用（DeepSeek API 模式下无需加载本地权重）。"""
        self.model = "api"
        self.tokenizer = None
        print("Using DeepSeek API provider, skip local model loading.")

    # ---------------------
    # 同步接口
    # ---------------------

    def generate(
        self,
        prompt: str,
        system_prompt: str,
        max_new_tokens: int = 128,
        model: Optional[str] = None,
        thinking_enabled: bool = True,
    ) -> str:
        client = self._get_sync_client()
        return client.generate(
            prompt, system_prompt, max_new_tokens,
            model_override=model, thinking_enabled=thinking_enabled,
        )

    def generate_stream(
        self,
        prompt: str,
        system_prompt: str,
        max_new_tokens: int = 128,
        model: Optional[str] = None,
        thinking_enabled: bool = True,
    ):
        client = self._get_sync_client()
        return client.generate_stream(
            prompt, system_prompt, max_new_tokens,
            model_override=model, thinking_enabled=thinking_enabled,
        )

    def generate_with_tools(
        self,
        messages: List[Dict[str, Any]],
        tools: List[Dict[str, Any]],
        max_new_tokens: int = 4096,
        tool_choice: Optional[str] = None,
        model: Optional[str] = None,
        temperature: Optional[float] = None,
        thinking_enabled: bool = True,
    ) -> ToolCallResult:
        client = self._get_sync_client()
        return client.generate_with_tools(
            messages=messages, tools=tools, max_new_tokens=max_new_tokens,
            tool_choice=tool_choice, model_override=model,
            temperature=temperature, thinking_enabled=thinking_enabled,
        )

    def generate_with_tools_stream(
        self,
        messages: List[Dict[str, Any]],
        tools: List[Dict[str, Any]],
        max_new_tokens: int = 4096,
        tool_choice: Optional[str] = None,
        model: Optional[str] = None,
        temperature: Optional[float] = None,
        thinking_enabled: bool = True,
    ) -> Generator[Dict[str, Any], None, None]:
        client = self._get_sync_client()
        return client.generate_with_tools_stream(
            messages=messages, tools=tools, max_new_tokens=max_new_tokens,
            tool_choice=tool_choice, model_override=model,
            temperature=temperature, thinking_enabled=thinking_enabled,
        )

    # ---------------------
    # 异步接口
    # ---------------------

    async def generate_async(
        self,
        prompt: str,
        system_prompt: str,
        max_new_tokens: int = 128,
        model: Optional[str] = None,
        thinking_enabled: bool = True,
    ) -> str:
        client = await self._get_async_client()
        return await client.generate(
            prompt, system_prompt, max_new_tokens,
            model_override=model, thinking_enabled=thinking_enabled,
        )

    async def generate_stream_async(
        self,
        prompt: str,
        system_prompt: str,
        max_new_tokens: int = 128,
        model: Optional[str] = None,
        thinking_enabled: bool = True,
    ) -> AsyncGenerator[Dict[str, Any], None]:
        client = await self._get_async_client()
        async for chunk in client.generate_stream(
            prompt, system_prompt, max_new_tokens,
            model_override=model, thinking_enabled=thinking_enabled,
        ):
            yield chunk

    async def close_async(self) -> None:
        if self._async_client is not None:
            await self._async_client.close()
            self._async_client = None

    async def generate_with_tools_async(
        self,
        messages: List[Dict[str, Any]],
        tools: List[Dict[str, Any]],
        max_new_tokens: int = 4096,
        tool_choice: Optional[str] = None,
        model: Optional[str] = None,
        temperature: Optional[float] = None,
        thinking_enabled: bool = True,
    ) -> ToolCallResult:
        client = await self._get_async_client()
        return await client.generate_with_tools(
            messages=messages, tools=tools, max_new_tokens=max_new_tokens,
            tool_choice=tool_choice, model_override=model,
            temperature=temperature, thinking_enabled=thinking_enabled,
        )

    async def generate_with_tools_stream_async(
        self,
        messages: List[Dict[str, Any]],
        tools: List[Dict[str, Any]],
        max_new_tokens: int = 4096,
        tool_choice: Optional[str] = None,
        model: Optional[str] = None,
        temperature: Optional[float] = None,
        thinking_enabled: bool = True,
    ) -> AsyncGenerator[Dict[str, Any], None]:
        client = await self._get_async_client()
        async for chunk in client.generate_with_tools_stream(
            messages=messages, tools=tools, max_new_tokens=max_new_tokens,
            tool_choice=tool_choice, model_override=model,
            temperature=temperature, thinking_enabled=thinking_enabled,
        ):
            yield chunk


__all__ = ["DeepSeekEngine", "ToolCallResult"]
```

- [ ] **Step 2: Verify the file is syntactically valid Python**

Run: `python -c "import ast; ast.parse(open('../Semantic-Atlas/backend_inference/core/engine/__init__.py').read()); print('OK')"`
Expected: `OK`

- [ ] **Step 3: Commit**

```bash
git -C ../Semantic-Atlas add backend_inference/core/engine/__init__.py
git -C ../Semantic-Atlas commit -m "refactor: simplify DeepSeekEngine to hardcode deepseek provider, remove local/chutes branches"
```

---

### Task 5: Simplify config.py — remove local and chutes configuration

**Files:**
- Modify: `Semantic-Atlas/backend_inference/core/config.py`

- [ ] **Step 1: Write the simplified config.py**

Replace the entire file content. Key changes:
- Remove `_resolve_model_provider()` — hardcode to `"deepseek"`
- Remove `CLASSIFICATION_PROVIDER` separate config — hardcode to `"deepseek"`
- Remove `SKIP_MODEL_LOAD` — no longer applicable without local models
- Remove `MODEL_PATH`, `MODEL_RELATIVE_PATH` — no local model weights
- Remove `CHUTES_API_URL`, `CHUTES_API_TOKEN` — no chutes provider
- Remove `CLASSIFICATION_MODEL_PATH`, `CLASSIFICATION_MODEL_RELATIVE_PATH`, `CLASSIFICATION_LABEL_ENCODER_PATH` — classification via API only
- Remove `CLASSIFICATION_BATCH_SIZE`, `CLASSIFICATION_MAX_SEQ_LEN` — not needed for API-based classification
- Keep `SKIP_KB_LOAD` (still used by KB, though disabled by default)
- Keep DeepSeek config
- Keep OpenAI compatibility config

```python
import json
import os

# 尝试自动加载项目根目录的 .env，便于本地开发/测试
try:
    from dotenv import load_dotenv  # type: ignore
except ImportError:  # 若未安装则忽略，由 requirements 安装后生效
    load_dotenv = None

if load_dotenv:
    _ROOT_ENV_PATH = os.path.join(
        os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))),
        ".env",
    )
    load_dotenv(_ROOT_ENV_PATH, override=False)


class Settings:
    # 获取项目根目录 (假设结构为: root/backend_inference/core/config.py)
    BASE_DIR = os.path.dirname(
        os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    )

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
```

- [ ] **Step 2: Verify the file is syntactically valid Python**

Run: `python -c "from Semantic-Atlas.backend_inference.core.config import settings; print('MODEL_PROVIDER:', settings.MODEL_PROVIDER)"`
Expected: `MODEL_PROVIDER: deepseek`

- [ ] **Step 3: Commit**

```bash
git -C ../Semantic-Atlas add backend_inference/core/config.py
git -C ../Semantic-Atlas commit -m "refactor: simplify config — hardcode deepseek, remove local/chutes/classification model paths"
```

---

### Task 6: Simplify model_container.py — remove chutes params

**Files:**
- Modify: `Semantic-Atlas/backend_inference/core/model_container.py`

- [ ] **Step 1: Write the simplified model_container.py**

Replace the entire file content. The `DeepSeekEngine` constructor no longer takes `model_path`, `provider`, `chutes_*` params — only deepseek params. Same for `RequirementClassificationEngine`.

```python
from .engine import DeepSeekEngine
from .engine.remote.deepseek import DEFAULT_DEEPSEEK_MODEL
from .config import settings


model_engine = DeepSeekEngine(
    deepseek_api_url=settings.DEEPSEEK_API_URL,
    deepseek_api_key=settings.DEEPSEEK_API_KEY,
    deepseek_model=getattr(settings, "DEEPSEEK_MODEL", DEFAULT_DEEPSEEK_MODEL),
)


class _LazyClassificationEngine:
    def __init__(self) -> None:
        self._engine = None

    def _get_engine(self):
        if self._engine is None:
            from .classification_engine import RequirementClassificationEngine

            self._engine = RequirementClassificationEngine(
                provider=settings.CLASSIFICATION_PROVIDER,
                deepseek_api_url=settings.DEEPSEEK_API_URL,
                deepseek_api_key=settings.DEEPSEEK_API_KEY,
                deepseek_model=getattr(settings, "DEEPSEEK_MODEL", DEFAULT_DEEPSEEK_MODEL),
                labels=settings.CLASSIFICATION_LABELS,
                other_label=settings.CLASSIFICATION_OTHER_LABEL,
            )
        return self._engine

    def __getattr__(self, name):
        return getattr(self._get_engine(), name)


classification_engine = _LazyClassificationEngine()
```

- [ ] **Step 2: Commit**

```bash
git -C ../Semantic-Atlas add backend_inference/core/model_container.py
git -C ../Semantic-Atlas commit -m "refactor: simplify model_container — remove chutes params from engine/classifier"
```

---

### Task 7: Simplify classification_engine.py — remove local HF model loading

**Files:**
- Modify: `Semantic-Atlas/backend_inference/core/classification_engine.py`

- [ ] **Step 1: Write the simplified classification_engine.py**

Replace the entire file content. Remove local model loading (transformers, torch, joblib), remove chutes client, keep only deepseek-based classification. Since there's only one provider, inline the remote prediction directly.

```python
import json
import re
from typing import List, Optional

from .engine.remote import DeepSeekClient
from .engine.remote.deepseek import DEFAULT_DEEPSEEK_MODEL

DEFAULT_CLASSIFICATION_LABELS = [
    "功能性需求",
    "性能需求",
    "安全需求",
    "可靠性需求",
    "可用性需求",
    "兼容性需求",
    "可维护性需求",
    "其他",
]

SYSTEM_PROMPT_CLASSIFICATION = """你是资深需求工程专家，负责将需求文本归类到给定标签中。
必须严格输出有效 JSON，且 label 只能取自提供的标签列表。
不要输出任何多余解释、Markdown 或前后缀。"""


def _extract_first_json_object(text: str) -> Optional[dict]:
    if not text:
        return None
    s = text.strip()
    try:
        obj = json.loads(s)
        if isinstance(obj, dict):
            return obj
    except Exception:
        pass

    s = s.replace("```json", "").replace("```JSON", "").replace("```", "").strip()
    m = re.search(r"\{.*\}", s, re.DOTALL)
    if not m:
        return None
    try:
        obj = json.loads(m.group(0))
        return obj if isinstance(obj, dict) else None
    except Exception:
        return None


class RequirementClassificationEngine:
    """DeepSeek API-based classifier for requirement categories."""

    def __init__(
        self,
        provider: str = "deepseek",
        deepseek_api_url: Optional[str] = None,
        deepseek_api_key: Optional[str] = None,
        deepseek_model: Optional[str] = None,
        labels: Optional[List[str]] = None,
        other_label: str = "其他",
    ) -> None:
        self.provider = provider or "deepseek"
        self.deepseek_api_url = deepseek_api_url or ""
        self.deepseek_api_key = deepseek_api_key
        self.deepseek_model = deepseek_model or DEFAULT_DEEPSEEK_MODEL
        self._remote_client = None
        self._user_labels = self._normalize_label_list(labels)
        self.other_label = other_label or "其他"
        self.labels: List[str] = []

    @property
    def is_ready(self) -> bool:
        return True

    def load(self) -> None:
        """Initialize labels (no local model to load)."""
        self._resolve_label_list()
        print("Classification engine ready (DeepSeek API mode).")

    def _ensure_ready(self) -> None:
        if not self.labels:
            self._resolve_label_list()

    def _normalize_label_list(self, labels: Optional[List[str]]) -> List[str]:
        if not labels:
            return []
        raw_list: List[str] = []
        if isinstance(labels, str):
            try:
                parsed = json.loads(labels)
                if isinstance(parsed, list):
                    raw_list = [str(item) for item in parsed]
                else:
                    raw_list = [s for s in labels.split(",")]
            except json.JSONDecodeError:
                raw_list = [s for s in labels.split(",")]
        else:
            raw_list = [str(item) for item in labels]

        seen = set()
        normalized: List[str] = []
        for item in raw_list:
            cleaned = str(item).strip()
            if not cleaned or cleaned in seen:
                continue
            seen.add(cleaned)
            normalized.append(cleaned)
        return normalized

    def _resolve_label_list(self) -> None:
        if self._user_labels:
            labels = list(self._user_labels)
        else:
            labels = list(DEFAULT_CLASSIFICATION_LABELS)

        if self.other_label and self.other_label not in labels:
            labels.append(self.other_label)

        seen = set()
        deduped: List[str] = []
        for label in labels:
            cleaned = str(label).strip()
            if not cleaned or cleaned in seen:
                continue
            seen.add(cleaned)
            deduped.append(cleaned)
        self.labels = deduped

    def _get_remote_client(self) -> DeepSeekClient:
        if self._remote_client is None:
            self._remote_client = DeepSeekClient(
                self.deepseek_api_url,
                self.deepseek_api_key,
                model=self.deepseek_model,
            )
        return self._remote_client

    def _build_prompt(self, requirement: str, labels: List[str]) -> str:
        label_text = "，".join(labels)
        return (
            "请将以下需求文本归类到给定标签之一。\n"
            f"标签列表：{label_text}\n"
            f"需求文本：{requirement}\n"
            "仅输出 JSON，例如：{\"label\":\"性能需求\",\"confidence\":0.78}\n"
            f"若无法判断，请输出 {self.other_label}。"
        )

    def _match_label_in_text(self, text: str, labels: List[str]) -> Optional[str]:
        if not text:
            return None
        for label in labels:
            if label and label in text:
                return label
        for label in labels:
            if label.endswith("需求") and label[:-2] and label[:-2] in text:
                return label
            if not label.endswith("需求") and f"{label}需求" in text:
                return label
        return None

    def _parse_remote_label(self, raw: str, labels: List[str]) -> str:
        parsed = _extract_first_json_object(raw)
        label = None
        if parsed:
            for key in ("label", "predicted_label", "category", "class"):
                if key in parsed:
                    label = parsed.get(key)
                    break

        if label is not None:
            label = str(label).strip().strip('"').strip("'")
            if label in labels:
                return label
            match = self._match_label_in_text(label, labels)
            if match:
                return match

        match = self._match_label_in_text(raw, labels)
        return match or (self.other_label if self.other_label else labels[-1])

    def predict(
        self,
        texts: List[str],
        batch_size: Optional[int] = None,
        max_length: Optional[int] = None,
    ) -> List[str]:
        """Classify a batch of requirements via DeepSeek API."""
        self._ensure_ready()
        if not texts:
            return []

        labels = self.labels or list(DEFAULT_CLASSIFICATION_LABELS)
        if self.other_label and self.other_label not in labels:
            labels.append(self.other_label)

        client = self._get_remote_client()
        predictions: List[str] = []
        for text in texts:
            item = "" if text is None else str(text).strip()
            if max_length:
                item = item[:max_length]
            if not item:
                predictions.append(self.other_label)
                continue
            prompt = self._build_prompt(item, labels)
            raw = client.generate(
                prompt=prompt,
                system_prompt=SYSTEM_PROMPT_CLASSIFICATION,
                max_new_tokens=96,
            )
            predictions.append(self._parse_remote_label(raw, labels))
        return predictions
```

- [ ] **Step 2: Commit**

```bash
git -C ../Semantic-Atlas add backend_inference/core/classification_engine.py
git -C ../Semantic-Atlas commit -m "refactor: remove local HF model loading from classification engine, deepseek API only"
```

---

### Task 8: Simplify main.py — remove local model startup logic

**Files:**
- Modify: `Semantic-Atlas/backend_inference/main.py`

- [ ] **Step 1: Write the simplified main.py**

Replace the entire file content. Key removals:
- `_should_skip_local_provider_load()` function
- `_initialize_generation_model()` local model branch
- `_initialize_classification_model()` local model branch
- Skip local model loading logic in lifespan

```python
import asyncio
import logging
import os
import sys
from contextlib import asynccontextmanager
from typing import Callable, Optional
from fastapi import FastAPI

# 确保可以从本地模块导入
current_dir = os.path.dirname(os.path.abspath(__file__))
if current_dir not in sys.path:
    sys.path.insert(0, current_dir)

from core.model_container import classification_engine, model_engine
from core.config import settings
from routers import (
    conflict,
    traceability,
    classification,
    requirements_acquisition,
    chat,
)
from routers import kb, l4_generation, l4_validation

# KB Service 导入
from services.kb_service import get_kb_service


logger = logging.getLogger(__name__)


def _is_windows_connection_reset_noise(context: dict) -> bool:
    message = str(context.get("message") or "")
    handle_repr = str(context.get("handle") or "")
    exception = context.get("exception")

    if not isinstance(exception, ConnectionResetError):
        return False
    if getattr(exception, "winerror", None) != 10054:
        return False

    target = "_ProactorBasePipeTransport._call_connection_lost"
    return target in message or target in handle_repr


def _install_windows_asyncio_noise_filter() -> Optional[Callable[[], None]]:
    if sys.platform != "win32":
        return None

    loop = asyncio.get_running_loop()
    previous_handler = loop.get_exception_handler()

    def _handler(active_loop, context):
        if _is_windows_connection_reset_noise(context):
            logger.debug(
                "Suppressed Windows Proactor connection reset noise during transport cleanup: %s",
                context.get("message") or context.get("exception"),
            )
            return
        if previous_handler is not None:
            previous_handler(active_loop, context)
            return
        active_loop.default_exception_handler(context)

    loop.set_exception_handler(_handler)

    def _restore() -> None:
        loop.set_exception_handler(previous_handler)

    return _restore


def _initialize_engines() -> None:
    """初始化推理引擎（DeepSeek API 模式，无需加载本地权重）。"""
    print(f"Initializing inference engine (provider=deepseek, model={settings.DEEPSEEK_MODEL})")
    try:
        model_engine.load_model()
    except Exception as e:
        print(f"CRITICAL ERROR: Failed to initialize model engine: {e}")

    print("Initializing classification engine (provider=deepseek)")
    try:
        classification_engine.load()
    except Exception as e:
        print(f"CRITICAL ERROR: Failed to initialize classification engine: {e}")


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    FastAPI 生命周期管理器
    """
    restore_asyncio_handler = _install_windows_asyncio_noise_filter()

    # 启动时：初始化引擎
    _initialize_engines()

    # 加载 KB 索引
    if settings.SKIP_KB_LOAD:
        print("[lifespan] SKIP_KB_LOAD is true, skipping KB index loading.")
        os.environ.setdefault("HF_HUB_OFFLINE", "1")
        os.environ.setdefault("TRANSFORMERS_OFFLINE", "1")
    else:
        print("Initializing Knowledge Base indexes...")
        try:
            kb_service = get_kb_service()
            kb_version = kb_service.ensure_loaded()
            print(f"KB indexes loaded successfully, version: {kb_version}")
        except Exception as e:
            print(f"WARNING: Failed to load KB indexes: {e}")

    try:
        yield
    finally:
        if restore_asyncio_handler is not None:
            restore_asyncio_handler()


# 创建 FastAPI 应用实例
app = FastAPI(
    title="Semantic Atlas Inference API",
    version="1.0",
    description="Backend inference service for requirement analysis (DeepSeek API).",
    lifespan=lifespan,
)

from fastapi.middleware.cors import CORSMiddleware

# 添加 CORS 中间件，允许所有来源访问
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 注册路由 (Routers)
app.include_router(conflict.router)
app.include_router(traceability.router)
app.include_router(classification.router)
app.include_router(requirements_acquisition.router)
app.include_router(chat.router)

# L4 需求生成相关路由
app.include_router(kb.router)
app.include_router(l4_generation.router)
app.include_router(l4_validation.router)
app.include_router(l4_generation.root_router)
app.include_router(l4_validation.root_router)


@app.get("/health")
def health_check():
    """
    健康检查接口
    检查引擎是否已初始化
    """
    return {
        "status": "ok",
        "model_loaded": model_engine.model is not None,
        "classifier_loaded": classification_engine.is_ready,
    }


if __name__ == "__main__":
    import uvicorn

    # 启动 uvicorn 服务器
    uvicorn.run(app, host="0.0.0.0", port=8000)
```

- [ ] **Step 2: Commit**

```bash
git -C ../Semantic-Atlas add backend_inference/main.py
git -C ../Semantic-Atlas commit -m "refactor: remove local model startup logic from main.py"
```

---

### Task 9: Slim down requirements.txt — remove local model dependencies

**Files:**
- Modify: `Semantic-Atlas/backend_inference/requirements.txt`

- [ ] **Step 1: Write the slimmed requirements.txt**

Replace the entire file content. Remove `torch`, `transformers`, `sentence-transformers`, `accelerate`, `faiss-cpu`, `llama-index-core`, `llama-index-embeddings-huggingface`, `llama-index-retrievers-bm25`. Keep `httpx`, `aiohttp`, `requests`, `openai` for API calls. Keep `pandas`, `numpy`, `joblib` for data processing.

```txt
# Semantic-Atlas Inference Backend Dependencies
# DeepSeek API only — no local model dependencies

fastapi>=0.115.0
uvicorn[standard]>=0.30.0
pydantic>=2.0.0
pydantic-settings>=2.0.0

# HTTP Clients
httpx>=0.27.0
aiohttp>=3.9.0
requests>=2.31.0
openai>=1.30.0

# Database
psycopg2-binary>=2.9.0
pymysql>=1.1.0

# Auth
PyJWT>=2.8.0
bcrypt>=4.1.0

# Data Processing
pandas>=2.0.0
numpy>=1.24.0
joblib>=1.3.0

# Logging
python-json-logger>=2.0.0
```

- [ ] **Step 2: Commit**

```bash
git -C ../Semantic-Atlas add backend_inference/requirements.txt
git -C ../Semantic-Atlas commit -m "refactor: remove local model deps (torch, transformers, faiss, llama-index) from requirements"
```

---

### Task 10: Update .env.example — remove stale config keys

**Files:**
- Modify: `cloudys/.env.example`

- [ ] **Step 1: Replace the Python Sidecar / LLM section**

Replace the current section:
```
# Python Sidecar / LLM
MODEL_PROVIDER=deepseek
DEEPSEEK_API_KEY=
DEEPSEEK_API_URL=https://api.deepseek.com
SKIP_MODEL_LOAD=true
SKIP_KB_LOAD=true
```

With:
```
# Python Sidecar / LLM (DeepSeek API)
DEEPSEEK_API_KEY=
DEEPSEEK_API_URL=https://api.deepseek.com
DEEPSEEK_MODEL=deepseek-v4-pro
SKIP_KB_LOAD=true
```

- [ ] **Step 2: Commit**

```bash
git add .env.example
git commit -m "chore: remove SKIP_MODEL_LOAD and MODEL_PROVIDER from .env.example"
```

---

### Task 11: Update docker-compose.yml — remove stale env vars

**Files:**
- Modify: `cloudys/docker-compose.yml`

- [ ] **Step 1: Replace the python-sidecar environment block**

Replace lines 49-54:
```yaml
    environment:
      - MODEL_PROVIDER=${MODEL_PROVIDER:-deepseek}
      - DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY:-}
      - DEEPSEEK_API_URL=${DEEPSEEK_API_URL:-https://api.deepseek.com}
      - SKIP_MODEL_LOAD=${SKIP_MODEL_LOAD:-true}
      - SKIP_KB_LOAD=${SKIP_KB_LOAD:-true}
```

With:
```yaml
    environment:
      - DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY:-}
      - DEEPSEEK_API_URL=${DEEPSEEK_API_URL:-https://api.deepseek.com}
      - DEEPSEEK_MODEL=${DEEPSEEK_MODEL:-deepseek-v4-pro}
      - SKIP_KB_LOAD=${SKIP_KB_LOAD:-true}
```

- [ ] **Step 2: Commit**

```bash
git add docker-compose.yml
git commit -m "chore: remove MODEL_PROVIDER and SKIP_MODEL_LOAD from docker-compose"
```

---

### Task 12: Update K8s inference-service deployment — remove stale env vars

**Files:**
- Modify: `cloudys/deploy/k8s/09-inference-service.yml`

- [ ] **Step 1: Replace the python-sidecar container env block**

Replace lines 81-92:
```yaml
          env:
            - name: MODEL_PROVIDER
              value: "${MODEL_PROVIDER:-deepseek}"
            - name: DEEPSEEK_API_KEY
              valueFrom:
                secretKeyRef:
                  name: deepseek-secret
                  key: DEEPSEEK_API_KEY
                  optional: true
            - name: SKIP_MODEL_LOAD
              value: "true"
            - name: SKIP_KB_LOAD
              value: "true"
```

With:
```yaml
          env:
            - name: DEEPSEEK_API_KEY
              valueFrom:
                secretKeyRef:
                  name: deepseek-secret
                  key: DEEPSEEK_API_KEY
                  optional: true
            - name: DEEPSEEK_API_URL
              value: "https://api.deepseek.com"
            - name: DEEPSEEK_MODEL
              value: "deepseek-v4-pro"
            - name: SKIP_KB_LOAD
              value: "true"
```

- [ ] **Step 2: Commit**

```bash
git add deploy/k8s/09-inference-service.yml
git commit -m "chore: remove MODEL_PROVIDER and SKIP_MODEL_LOAD from K8s inference deployment"
```

---

### Task 13: Fix frontend "本地模型" label in RequirementGraphView.vue

**Files:**
- Modify: `cloudys/frontend/src/views/beta/RequirementGraphView.vue`

- [ ] **Step 1: Fix the "本地模型" label (line 288)**

The template line:
```html
<span class="analysis-chip">{{ localVectorSummary.model || '本地模型' }}</span>
```

Changes to:
```html
<span class="analysis-chip">{{ localVectorSummary.model || 'DeepSeek' }}</span>
```

- [ ] **Step 2: Fix the computed property name from `localVectorSummary` to `vectorSummary`**

The computed property at line 696 is named `localVectorSummary`. This is a cosmetic rename to avoid confusion. Rename `localVectorSummary` to `vectorSummary` throughout the file.

Search and replace all occurrences:
- `const localVectorSummary = computed(` → `const vectorSummary = computed(`
- `v-if="localVectorSummary"` → `v-if="vectorSummary"`
- `{{ localVectorSummary.model` → `{{ vectorSummary.model`
- `{{ localVectorSummary.candidate_pair_count` → `{{ vectorSummary.candidate_pair_count`
- `{{ localVectorSummary.reviewed_pair_count` → `{{ vectorSummary.reviewed_pair_count`
- `{{ localVectorSummary.added_count` → `{{ vectorSummary.added_count`

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/beta/RequirementGraphView.vue
git commit -m "refactor: rename localVectorSummary to vectorSummary, fix local model label"
```

---

## Verification Checklist

After all tasks are complete, verify:

1. **Python syntax check**: `python -c "import ast; ast.parse(open('../Semantic-Atlas/backend_inference/main.py').read()); print('OK')"`
2. **Config loads**: `python -c "import sys; sys.path.insert(0,'../Semantic-Atlas/backend_inference'); from core.config import settings; assert settings.MODEL_PROVIDER == 'deepseek'; print('OK')"`
3. **Engine instantiates**: `python -c "import sys; sys.path.insert(0,'../Semantic-Atlas/backend_inference'); from core.model_container import model_engine; print('Engine OK')"`
4. **No local model imports remain**: `grep -r "LocalHFEngine\|from.*local\|provider.*local\|ChutesClient\|AsyncChutesClient\|from.*chutes" ../Semantic-Atlas/backend_inference/core/` should return no results (or only in comments)
5. **Frontend builds**: `cd frontend && npm run build`
6. **DeepSeek API key set**: Ensure `DEEPSEEK_API_KEY` is configured in `.env` before running
7. **End-to-end smoke test**: Start the inference service and call `POST /chat/completions` with a test prompt

---

## Self-Review

### 1. Spec Coverage
- ✅ Remove local HuggingFace model loading → Tasks 1, 7, 8
- ✅ Remove Chutes API client → Tasks 2, 3, 4
- ✅ Hardcode DeepSeek as sole provider → Tasks 4, 5, 6
- ✅ Remove local model dependencies from requirements.txt → Task 9
- ✅ Clean up environment configs → Tasks 10, 11, 12
- ✅ Fix frontend references → Task 13
- ✅ Classification only via DeepSeek API → Task 7

### 2. Placeholder Scan
- No TBD, TODO, or "implement later" found
- All code blocks contain complete implementations
- All commands have expected output specified

### 3. Type Consistency
- `DeepSeekEngine.__init__` signature in Task 4 matches `model_container.py` instantiation in Task 6
- `RequirementClassificationEngine.__init__` signature in Task 7 matches `model_container.py` instantiation in Task 6
- `Settings` class in Task 5 has all attributes referenced by Tasks 6, 7, 8
