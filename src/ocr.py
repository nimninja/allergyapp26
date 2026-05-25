"""EasyOCR-based text extraction from images (bytes or PIL)."""

from __future__ import annotations

from io import BytesIO

import numpy as np
from PIL import Image


def _image_from_bytes(data: bytes) -> Image.Image:
    img = Image.open(BytesIO(data))
    if img.mode not in ("RGB", "L"):
        img = img.convert("RGB")
    return img


def get_easyocr_reader():
    """Lazy import so tests can mock without easyocr installed."""
    import easyocr

    return easyocr.Reader(["en"], gpu=False)


def extract_text_from_image(image_bytes: bytes, reader=None) -> str:
    """
    Run OCR on image bytes and return newline-joined text lines in reading order.
    Pass ``reader`` to reuse a cached Reader instance (e.g. from the desktop app).
    """
    if reader is None:
        reader = get_easyocr_reader()
    img = _image_from_bytes(image_bytes)
    arr = np.array(img)
    # EasyOCR expects numpy array; RGB or grayscale
    if arr.ndim == 2:
        pass
    elif arr.shape[2] == 4:
        arr = arr[:, :, :3]
    results = reader.readtext(arr)
    lines = [item[1] for item in results if item[1] and str(item[1]).strip()]
    return "\n".join(lines)
