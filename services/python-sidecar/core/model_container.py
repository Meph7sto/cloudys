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
