"""Tests for preference parsing."""

from __future__ import annotations

import pytest

from src.preferences import parse_allergen_ids, parse_extra_avoid


def test_parse_extra_avoid_splits_commas_and_lines() -> None:
    assert parse_extra_avoid("coconut, mustard\nMSG") == ["coconut", "mustard", "MSG"]


def test_parse_allergen_ids_deduplicates() -> None:
    assert parse_allergen_ids("milk, peanuts, milk") == ["milk", "peanuts"]


def test_parse_allergen_ids_rejects_unknown() -> None:
    with pytest.raises(ValueError, match="Unknown allergen"):
        parse_allergen_ids("not_an_allergen")
