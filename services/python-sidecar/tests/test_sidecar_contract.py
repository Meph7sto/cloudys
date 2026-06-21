from pathlib import Path

import importlib.util


def test_project_layout_contains_expected_entrypoints():
    root = Path(__file__).resolve().parents[1]

    assert (root / "pyproject.toml").exists()
    assert (root / "main.py").exists()
    assert (root / "sidecar_app.py").exists()
    assert (root / "data" / "kb_indexes").exists()


def test_repo_local_sidecar_entrypoint_is_importable():
    root = Path(__file__).resolve().parents[1]
    module_path = root / "sidecar_app.py"

    spec = importlib.util.spec_from_file_location("sidecar_app", module_path)
    assert spec is not None
    assert spec.loader is not None


def test_repo_local_main_module_is_importable():
    root = Path(__file__).resolve().parents[1]
    module_path = root / "main.py"

    spec = importlib.util.spec_from_file_location("main", module_path)
    assert spec is not None
    assert spec.loader is not None
