"""Parse restriction preferences from form fields or UI text."""

from __future__ import annotations

from src.constants import ALLERGEN_IDS


def parse_extra_avoid(text: str) -> list[str]:
    if not text or not text.strip():
        return []
    parts: list[str] = []
    for line in text.replace(",", "\n").splitlines():
        t = line.strip()
        if t:
            parts.append(t)
    return parts


def parse_allergen_ids(text: str) -> list[str]:
    """Comma- or newline-separated allergen ids (e.g. ``milk,peanuts``)."""
    if not text or not text.strip():
        return []
    selected: list[str] = []
    seen: set[str] = set()
    for part in text.replace("\n", ",").split(","):
        aid = part.strip().lower()
        if not aid or aid in seen:
            continue
        if aid not in ALLERGEN_IDS:
            raise ValueError(f"Unknown allergen id: {aid}")
        seen.add(aid)
        selected.append(aid)
    return selected
