"""Scan pipeline: OCR image bytes, then match against YAML rules."""

from __future__ import annotations

from pathlib import Path
from typing import Any, Literal

from src.constants import DISCLAIMER
from src.match import MatchResult, load_default_rules_dir, match_text
from src.ocr import extract_text_from_image

SummaryLevel = Literal["no_text", "violation", "warning", "ok"]


def summarize_result(result: MatchResult) -> tuple[str, SummaryLevel]:
    if not result.raw_text.strip():
        return (
            "No text could be read. Try better lighting or a sharper photo.",
            "no_text",
        )
    if result.violations:
        return (
            f"Potential violations: {len(result.violations)} — "
            "selected restrictions may not be met.",
            "violation",
        )
    if result.warnings:
        return (
            "No strong violations matched; review the items below.",
            "warning",
        )
    return (
        "No keyword violations matched (still verify the label yourself).",
        "ok",
    )


def match_result_to_dict(result: MatchResult) -> dict[str, Any]:
    summary, summary_level = summarize_result(result)
    return {
        "raw_text": result.raw_text,
        "normalized": result.normalized,
        "violations": result.violations,
        "warnings": result.warnings,
        "may_contain_notices": result.may_contain_notices,
        "summary": summary,
        "summary_level": summary_level,
        "disclaimer": DISCLAIMER,
    }


def scan_label_image(
    image_bytes: bytes,
    *,
    rules_dir: Path | None = None,
    selected_allergens: list[str],
    vegan: bool,
    vegetarian: bool,
    extra_avoid: list[str],
    ocr_reader=None,
) -> dict[str, Any]:
    rules = rules_dir or load_default_rules_dir()
    raw_text = extract_text_from_image(image_bytes, reader=ocr_reader)
    result = match_text(
        raw_text,
        rules_dir=rules,
        selected_allergens=selected_allergens,
        vegan=vegan,
        vegetarian=vegetarian,
        extra_avoid=extra_avoid,
    )
    return match_result_to_dict(result)
