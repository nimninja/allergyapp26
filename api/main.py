"""
Ingredient checker API — production-ready HTTP backend.

Local dev:
  uvicorn api.main:app --reload --host 0.0.0.0 --port 8000

Production (Docker, 512 MB friendly):
  OCR_ENGINE=tesseract (default in Dockerfile)
"""

from __future__ import annotations

import logging
import os
from contextlib import asynccontextmanager
from pathlib import Path
from typing import Any

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from slowapi import _rate_limit_exceeded_handler
from slowapi.errors import RateLimitExceeded
from slowapi.middleware import SlowAPIMiddleware

from api.config import settings
from api.limiter import limiter
from api.middleware import RequestContextMiddleware
from api.ocr_state import _load_backend, health_payload, init_ocr_state
from api.routes_v1 import router as v1_router
from src.ocr import ocr_engine

logging.basicConfig(level=settings.log_level)
logger = logging.getLogger("ingredient_checker")

_static_dir = Path(__file__).resolve().parent / "static"


def _preload_ocr_enabled() -> bool:
    if os.environ.get("PRELOAD_OCR", "").strip().lower() in {"1", "true", "yes"}:
        return True
    # Tesseract is light — safe to init at boot on 512 MB instances.
    return ocr_engine() == "tesseract"


@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.settings = settings
    init_ocr_state(app.state)
    if _preload_ocr_enabled():
        try:
            _load_backend(app.state)
        except Exception:
            pass  # errors stored on app.state; /health reports degraded
    else:
        logger.info("OCR (%s) will load on first /v1/scan", ocr_engine())
    yield


app = FastAPI(
    title="Ingredient checker API",
    description="Scan food label images against dietary restrictions.",
    version="1.0.0",
    lifespan=lifespan,
    docs_url="/docs" if not settings.is_production else None,
    redoc_url="/redoc" if not settings.is_production else None,
)

app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)
app.add_middleware(SlowAPIMiddleware)
app.add_middleware(RequestContextMiddleware)
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.allowed_origins,
    allow_credentials=True,
    allow_methods=["GET", "POST", "OPTIONS"],
    allow_headers=["*"],
)

app.include_router(v1_router)


@app.get("/")
def mobile_app() -> FileResponse:
    """Mobile-friendly web UI."""
    return FileResponse(_static_dir / "index.html")


@app.get("/health")
def health(request: Request) -> dict[str, Any]:
    """Load balancer health check (not rate limited)."""
    return health_payload(request.app.state)
