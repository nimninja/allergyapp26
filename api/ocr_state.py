"""OCR backend initialization for the API (Tesseract by default on Docker)."""

from __future__ import annotations

import logging
import threading
from typing import Any

from fastapi import HTTPException, Request

from src.ocr import init_ocr_backend, ocr_engine

logger = logging.getLogger("ingredient_checker")


def init_ocr_state(app_state: Any) -> None:
    app_state.ocr_reader = None
    app_state.ocr_error = None
    app_state.ocr_loading = False
    app_state.ocr_lock = threading.Lock()


def health_payload(app_state: Any) -> dict[str, Any]:
    reader = getattr(app_state, "ocr_reader", None)
    loading = getattr(app_state, "ocr_loading", False)
    ocr_error = getattr(app_state, "ocr_error", None)
    if reader is not None:
        status = "ok"
    elif loading:
        status = "starting"
    else:
        status = "degraded"
    return {
        "status": status,
        "ocr_ready": reader is not None,
        "ocr_loading": loading,
        "ocr_engine": ocr_engine(),
        "version": "1.0.0",
        "ocr_error": ocr_error if reader is None and not loading else None,
    }


def _load_backend(state: Any) -> Any:
    state.ocr_loading = True
    try:
        logger.info("Initializing OCR (%s)…", ocr_engine())
        state.ocr_reader = init_ocr_backend()
        state.ocr_error = None
        logger.info("OCR ready (%s)", ocr_engine())
        return state.ocr_reader
    except Exception as exc:
        state.ocr_reader = None
        state.ocr_error = str(exc)
        logger.exception("OCR engine failed to load")
        raise HTTPException(
            status_code=503,
            detail={
                "message": "OCR engine is not available on this server.",
                "error": state.ocr_error,
            },
        ) from exc
    finally:
        state.ocr_loading = False


def get_ocr_reader(request: Request) -> Any:
    state = request.app.state
    reader = getattr(state, "ocr_reader", None)
    if reader is not None:
        return reader

    lock = getattr(state, "ocr_lock", None)
    if lock is None:
        raise HTTPException(status_code=503, detail="OCR state not initialized.")

    with lock:
        reader = getattr(state, "ocr_reader", None)
        if reader is not None:
            return reader
        return _load_backend(state)
