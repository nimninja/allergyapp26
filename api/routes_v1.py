"""Versioned public API routes for mobile and third-party clients."""

import asyncio
from typing import Any

from fastapi import APIRouter, File, Form, HTTPException, Request, UploadFile

from api.config import settings
from api.limiter import limiter
from api.ocr_state import get_ocr_reader, health_payload
from src.constants import ALLERGEN_OPTIONS, DISCLAIMER
from src.preferences import parse_allergen_ids, parse_extra_avoid
from src.scan_service import scan_label_image

router = APIRouter(prefix="/v1", tags=["v1"])


@router.get("/health")
def health_v1(request: Request) -> dict[str, Any]:
    return health_payload(request.app.state)


@router.get("/allergens")
def allergens_v1() -> dict[str, Any]:
    return {
        "allergens": [{"id": key, "label": label} for key, label in ALLERGEN_OPTIONS],
        "disclaimer": DISCLAIMER,
    }


@router.post("/scan")
@limiter.limit(settings.rate_limit_scan)
async def scan_v1(
    request: Request,
    image: UploadFile = File(..., description="Label photo (JPEG, PNG, or WebP)"),
    allergens: str = Form("", description="Comma-separated allergen ids"),
    vegan: bool = Form(False),
    vegetarian: bool = Form(False),
    extra_avoid: str = Form("", description="Additional terms to avoid"),
) -> dict[str, Any]:
    app_settings = request.app.state.settings
    reader = get_ocr_reader(request)

    if not image.content_type or not image.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="Upload must be an image file.")

    data = await image.read()
    if not data:
        raise HTTPException(status_code=400, detail="Empty image upload.")
    if len(data) > app_settings.max_upload_bytes:
        raise HTTPException(
            status_code=413,
            detail=f"Image exceeds {app_settings.max_upload_bytes // (1024 * 1024)} MB limit.",
        )

    try:
        selected = parse_allergen_ids(allergens)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc

    try:
        # Tesseract is CPU-heavy; run off the event loop so /health stays responsive.
        return await asyncio.to_thread(
            scan_label_image,
            data,
            selected_allergens=selected,
            vegan=vegan,
            vegetarian=vegetarian,
            extra_avoid=parse_extra_avoid(extra_avoid),
            ocr_reader=reader,
        )
    except Exception as exc:
        detail = "Scan failed." if app_settings.is_production else f"Scan failed: {exc}"
        raise HTTPException(status_code=500, detail=detail) from exc
