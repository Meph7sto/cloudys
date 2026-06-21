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
