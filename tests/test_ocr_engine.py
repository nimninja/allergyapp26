"""Tests for OCR engine selection."""

from __future__ import annotations

import pytest

from src.ocr import _image_from_bytes, extract_text_from_image, ocr_engine


def test_ocr_engine_defaults_to_easyocr(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.delenv("OCR_ENGINE", raising=False)
    assert ocr_engine() == "easyocr"


def test_ocr_engine_tesseract_env(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("OCR_ENGINE", "tesseract")
    assert ocr_engine() == "tesseract"


def test_extract_tesseract_mocked(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("OCR_ENGINE", "tesseract")

    class FakePytesseract:
        @staticmethod
        def image_to_string(img, config=""):  # noqa: ARG004
            return "Ingredients: peanut butter"

    monkeypatch.setitem(
        __import__("sys").modules,
        "pytesseract",
        FakePytesseract(),
    )
    # Force import path inside function
    import src.ocr as ocr_mod

    monkeypatch.setattr(ocr_mod, "_extract_tesseract", lambda img: "Ingredients: peanut butter")
    png = (
        b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01"
        b"\x00\x00\x00\x01\x08\x02\x00\x00\x00\x90wS\xde\x00\x00\x00\x0cIDATx\x9cc\xf8\x0f\x00\x00\x01\x01\x00\x05\x18\xd8N\x00\x00\x00\x00IEND\xaeB`\x82"
    )
    text = extract_text_from_image(png, reader="tesseract")
    assert "peanut" in text.lower()
