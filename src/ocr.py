"""Text extraction from label images (Tesseract or EasyOCR)."""

from __future__ import annotations

import os
from io import BytesIO

from PIL import Image


def ocr_engine() -> str:
    """``tesseract`` (light, ~512 MB RAM) or ``easyocr`` (heavier, needs ~2 GB)."""
    return os.environ.get("OCR_ENGINE", "easyocr").strip().lower()


def _image_from_bytes(data: bytes) -> Image.Image:
    img = Image.open(BytesIO(data))
    if img.mode not in ("RGB", "L"):
        img = img.convert("RGB")
    return img


def _prepare_for_tesseract(img: Image.Image) -> Image.Image:
    """Upscale small crops so Tesseract reads ingredient lines more reliably."""
    w, h = img.size
    min_side = min(w, h)
    if min_side < 900:
        scale = 900 / min_side
        img = img.resize((int(w * scale), int(h * scale)), Image.Resampling.LANCZOS)
    return img


def init_ocr_backend():
    """
    Initialize OCR for the API process.
    Tesseract: verify binary is present (fast, low memory).
    EasyOCR: load neural models (slow, high memory).
    """
    engine = ocr_engine()
    if engine == "tesseract":
        import pytesseract

        pytesseract.get_tesseract_version()
        return "tesseract"
    if engine == "easyocr":
        return get_easyocr_reader()
    raise RuntimeError(f"Unknown OCR_ENGINE: {engine!r} (use tesseract or easyocr)")


def get_easyocr_reader():
    """Lazy import so tests can mock without easyocr installed."""
    import easyocr

    return easyocr.Reader(["en"], gpu=False)


def _extract_tesseract(img: Image.Image) -> str:
    import pytesseract

    img = _prepare_for_tesseract(img)
    # PSM 6 = single uniform block of text (typical ingredient panels)
    config = "--psm 6 -l eng"
    return pytesseract.image_to_string(img, config=config).strip()


def _extract_easyocr(img: Image.Image, reader) -> str:
    import numpy as np

    if reader is None:
        reader = get_easyocr_reader()
    arr = np.array(img)
    if arr.ndim == 2:
        pass
    elif arr.shape[2] == 4:
        arr = arr[:, :, :3]
    results = reader.readtext(arr)
    lines = [item[1] for item in results if item[1] and str(item[1]).strip()]
    return "\n".join(lines)


def extract_text_from_image(image_bytes: bytes, reader=None) -> str:
    """
    Run OCR on image bytes and return extracted text.
    ``reader`` is optional: EasyOCR Reader instance, or ``\"tesseract\"`` sentinel.
    """
    img = _image_from_bytes(image_bytes)
    use_tesseract = reader == "tesseract" or (
        reader is None and ocr_engine() == "tesseract"
    )
    if use_tesseract:
        return _extract_tesseract(img)
    return _extract_easyocr(img, reader)
