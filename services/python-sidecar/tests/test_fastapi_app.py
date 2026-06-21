from __future__ import annotations

from unittest.mock import Mock

import httpx
import main as sidecar_main
from asgi_lifespan import LifespanManager


async def test_health_endpoint_returns_expected_shape(monkeypatch):
    model_engine = Mock()
    model_engine.model = "api"
    classification_engine = Mock()
    classification_engine.is_ready = True

    monkeypatch.setattr(sidecar_main, "model_engine", model_engine)
    monkeypatch.setattr(sidecar_main, "classification_engine", classification_engine)
    monkeypatch.setattr(sidecar_main, "_initialize_engines", lambda: None)
    monkeypatch.setattr(sidecar_main.settings, "SKIP_KB_LOAD", True)

    transport = httpx.ASGITransport(app=sidecar_main.app)
    async with LifespanManager(sidecar_main.app):
        async with httpx.AsyncClient(transport=transport, base_url="http://testserver") as client:
            response = await client.get("/health")

    assert response.status_code == 200
    assert response.json() == {
        "status": "ok",
        "model_loaded": True,
        "classifier_loaded": True,
    }


def test_app_registers_expected_route_prefixes():
    paths = {route.path for route in sidecar_main.app.routes}

    expected = {
        "/health",
        "/chat/completions",
        "/classification/predict-texts",
        "/classification/predict-csv",
        "/conflict/check",
        "/traceability/relation",
        "/l4/generate",
        "/l4/validate",
        "/acquisition/extract",
        "/kb/search",
        "/kb/status",
    }

    assert expected.issubset(paths)
