import os
import sys
from pathlib import Path


def _resolve_backend_inference_root() -> Path:
    current = Path(__file__).resolve()
    candidates = [
        current.parents[4] / "Semantic-Atlas" / "backend_inference",
        current.parents[3] / "Semantic-Atlas" / "backend_inference",
    ]
    env_override = os.getenv("SEMANTIC_ATLAS_BACKEND_INFERENCE")
    if env_override:
        candidates.insert(0, Path(env_override).resolve())

    for candidate in candidates:
        if (candidate / "main.py").exists():
            return candidate

    raise RuntimeError(
        "Cannot locate Semantic-Atlas/backend_inference. "
        "Set SEMANTIC_ATLAS_BACKEND_INFERENCE or copy the backend_inference tree next to this bridge."
    )


BACKEND_INFERENCE_ROOT = _resolve_backend_inference_root()
backend_root_str = str(BACKEND_INFERENCE_ROOT)
if backend_root_str not in sys.path:
    sys.path.insert(0, backend_root_str)

from main import app  # noqa: E402
