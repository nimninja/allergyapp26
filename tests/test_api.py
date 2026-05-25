"""HTTP API tests (OCR mocked)."""

from __future__ import annotations

from io import BytesIO
from typing import Any

import pytest
from fastapi.testclient import TestClient

from api.config import settings as app_settings

import api.main as api_main
import api.routes_v1 as routes_v1


@pytest.fixture
def client(monkeypatch: pytest.MonkeyPatch) -> TestClient:
    def fake_scan(**kwargs: Any) -> dict[str, Any]:
        return {
            "raw_text": "Ingredients: peanut butter, salt.",
            "normalized": "ingredients peanut butter salt",
            "violations": [{"category": "Allergen: Peanuts", "keyword": "peanut"}],
            "warnings": [],
            "may_contain_notices": [],
            "summary": "Potential violations: 1 — selected restrictions may not be met.",
            "summary_level": "violation",
            "disclaimer": "This tool is not medical advice.",
        }

    api_main.limiter.enabled = False
    api_main.app.state.settings = app_settings
    api_main.app.state.ocr_reader = object()
    api_main.app.state.ocr_error = None
    monkeypatch.setattr(routes_v1, "scan_label_image", lambda *a, **k: fake_scan())
    return TestClient(api_main.app)


def test_health(client: TestClient) -> None:
    r = client.get("/health")
    assert r.status_code == 200
    assert r.json()["ocr_ready"] is True


def test_v1_health(client: TestClient) -> None:
    r = client.get("/v1/health")
    assert r.status_code == 200


def test_v1_allergens(client: TestClient) -> None:
    r = client.get("/v1/allergens")
    assert r.status_code == 200
    data = r.json()
    assert any(a["id"] == "peanuts" for a in data["allergens"])


def test_v1_scan_returns_json(client: TestClient) -> None:
    png = (
        b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01"
        b"\x00\x00\x00\x01\x08\x02\x00\x00\x00\x90wS\xde\x00\x00\x00\x0cIDATx\x9cc\xf8\x0f\x00\x00\x01\x01\x00\x05\x18\xd8N\x00\x00\x00\x00IEND\xaeB`\x82"
    )
    r = client.post(
        "/v1/scan",
        data={"allergens": "peanuts", "vegan": "false", "vegetarian": "false"},
        files={"image": ("label.png", BytesIO(png), "image/png")},
    )
    assert r.status_code == 200
    body = r.json()
    assert body["summary_level"] == "violation"
    assert body["violations"]


def test_v1_scan_rejects_unknown_allergen(client: TestClient) -> None:
    png = b"\x89PNG\r\n\x1a\n"
    r = client.post(
        "/v1/scan",
        data={"allergens": "bogus"},
        files={"image": ("x.png", BytesIO(png), "image/png")},
    )
    assert r.status_code == 400
