"""Tests for scan response shaping (no OCR)."""

from __future__ import annotations

from pathlib import Path

from src.match import match_text
from src.scan_service import match_result_to_dict, summarize_result


def test_match_result_to_dict_includes_summary(rules_dir: Path) -> None:
    result = match_text(
        "Ingredients: peanut butter.",
        rules_dir=rules_dir,
        selected_allergens=["peanuts"],
        vegan=False,
        vegetarian=False,
        extra_avoid=[],
    )
    payload = match_result_to_dict(result)
    assert payload["summary_level"] == "violation"
    assert payload["violations"]
    assert payload["disclaimer"]


def test_summarize_no_text() -> None:
    from src.match import MatchResult

    msg, level = summarize_result(MatchResult(raw_text="  ", normalized=""))
    assert level == "no_text"
    assert "No text" in msg
