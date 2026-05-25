"""
Ingredient checker API — production-ready HTTP backend.

Local dev:
  uvicorn api.main:app --reload --host 0.0.0.0 --port 8000

Production (Docker):
  docker build -t ingredient-checker-api .
  docker run -p 8000:8000 -e ENV=production ingredient-checker-api
"""

from __future__ import annotations

import logging
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
from api.routes_v1 import router as v1_router
from src.ocr import get_easyocr_reader

logging.basicConfig(level=settings.log_level)
logger = logging.getLogger("ingredient_checker")

_static_dir = Path(__file__).resolve().parent / "static"


@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.settings = settings
    try:
        app.state.ocr_reader = get_easyocr_reader()
        app.state.ocr_error = None
        logger.info("OCR engine ready")
    except Exception as exc:
        app.state.ocr_reader = None
        app.state.ocr_error = str(exc)
        logger.exception("OCR engine failed to load")
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
    reader = getattr(request.app.state, "ocr_reader", None)
    ocr_error = getattr(request.app.state, "ocr_error", None)
    return {
        "status": "ok" if reader is not None else "degraded",
        "ocr_ready": reader is not None,
        "version": "1.0.0",
        "ocr_error": ocr_error if reader is None else None,
    }
