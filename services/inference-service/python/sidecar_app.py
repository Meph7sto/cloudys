import sys
from pathlib import Path


SIDECAR_ROOT = Path(__file__).resolve().parents[2] / "python-sidecar"
if not (SIDECAR_ROOT / "main.py").exists():
    raise RuntimeError(f"Cannot locate repo-local python sidecar at '{SIDECAR_ROOT}'.")

sidecar_root_str = str(SIDECAR_ROOT)
if sidecar_root_str not in sys.path:
    sys.path.insert(0, sidecar_root_str)

from main import app  # noqa: E402
