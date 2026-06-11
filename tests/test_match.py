"""Tests for text normalization and rule matching."""

from __future__ import annotations

from pathlib import Path

from src.match import match_text, normalize_text


def test_normalize_text_collapses_whitespace_and_lower() -> None:
    assert normalize_text("  MILK, Whey!  ") == "milk whey"


def test_match_peanut_allergen(rules_dir: Path) -> None:
    text = "Ingredients: sugar, peanut butter, salt."
    r = match_text(
        text,
        rules_dir=rules_dir,
        selected_allergens=["peanuts"],
        vegan=False,
        vegetarian=False,
        extra_avoid=[],
    )
    assert any(v["keyword"] == "peanut" or v["keyword"] == "peanuts" for v in r.violations)


def test_match_milk_allergen_skips_when_not_selected(rules_dir: Path) -> None:
    text = "Contains milk and soy."
    r = match_text(
        text,
        rules_dir=rules_dir,
        selected_allergens=["soy"],
        vegan=False,
        vegetarian=False,
        extra_avoid=[],
    )
    categories = [v["category"] for v in r.violations]
    assert not any("Milk" in c for c in categories)


def test_vegan_violation_honey(rules_dir: Path) -> None:
    text = "Organic sugar, honey, vanilla."
    r = match_text(
        text,
        rules_dir=rules_dir,
        selected_allergens=[],
        vegan=True,
        vegetarian=False,
        extra_avoid=[],
    )
    assert any(v["category"] == "Vegan" and v["keyword"] == "honey" for v in r.violations)


def test_vegetarian_violation_bacon(rules_dir: Path) -> None:
    text = "Smoked bacon bits (pork), salt."
    r = match_text(
        text,
        rules_dir=rules_dir,
        selected_allergens=[],
        vegan=False,
        vegetarian=True,
        extra_avoid=[],
    )
    assert any(v["category"] == "Vegetarian" for v in r.violations)


def test_may_contain_detected(rules_dir: Path) -> None:
    text = "Sugar.\nMay contain traces of tree nuts."
    r = match_text(
        text,
        rules_dir=rules_dir,
        selected_allergens=[],
        vegan=False,
        vegetarian=False,
        extra_avoid=[],
    )
    assert r.may_contain_notices
    assert "may contain" in r.may_contain_notices[0].lower()


def test_extra_avoid_custom(rules_dir: Path) -> None:
    text = "Filtered water, coconut extract."
    r = match_text(
        text,
        rules_dir=rules_dir,
        selected_allergens=[],
        vegan=False,
        vegetarian=False,
        extra_avoid=["coconut"],
    )
    assert any("coconut" in v["keyword"].lower() for v in r.violations)


def test_word_boundary_egg_not_in_veggie(rules_dir: Path) -> None:
    text = "veggie chips, potato starch."
    r = match_text(
        text,
        rules_dir=rules_dir,
        selected_allergens=["eggs"],
        vegan=False,
        vegetarian=False,
        extra_avoid=[],
    )
    assert r.violations == []


def test_ocr_typo_glutan_matches_gluten(rules_dir: Path) -> None:
    text = "Ingredients: rice flour, glutan, salt."
    r = match_text(
        text,
        rules_dir=rules_dir,
        selected_allergens=[],
        vegan=False,
        vegetarian=False,
        extra_avoid=["gluten"],
    )
    assert any("gluten" in v["keyword"].lower() for v in r.violations)


def test_ocr_typo_whey_still_matches_milk_allergen(rules_dir: Path) -> None:
    text = "Contains whev and sugar."
    r = match_text(
        text,
        rules_dir=rules_dir,
        selected_allergens=["milk"],
        vegan=False,
        vegetarian=False,
        extra_avoid=[],
    )
    assert any("Milk" in v["category"] for v in r.violations)


def test_synonym_clarified_butter_matches_ghee(rules_dir: Path) -> None:
    text = "Ingredients: clarified butter, salt."
    r = match_text(
        text,
        rules_dir=rules_dir,
        selected_allergens=["milk"],
        vegan=False,
        vegetarian=False,
        extra_avoid=["ghee"],
    )
    assert any("ghee" in v["keyword"].lower() for v in r.violations)


def test_ocr_penauts_matches_peanut_via_preset(rules_dir: Path) -> None:
    text = "May contain penauts and tree nuts."
    r = match_text(
        text,
        rules_dir=rules_dir,
        selected_allergens=["peanuts"],
        vegan=False,
        vegetarian=False,
        extra_avoid=[],
    )
    assert any("peanut" in v["keyword"].lower() for v in r.violations)
    hit = next(v for v in r.violations if "peanut" in v["keyword"].lower())
    assert hit["match_method"] == "ocr_fix"
    assert hit["matched_text"] == "penauts"


def test_ocr_fix_reports_method(rules_dir: Path) -> None:
    text = "Ingredients: glutan, salt."
    r = match_text(
        text,
        rules_dir=rules_dir,
        selected_allergens=[],
        vegan=False,
        vegetarian=False,
        extra_avoid=["gluten"],
    )
    hit = next(v for v in r.violations if v["keyword"] == "gluten")
    assert hit["match_method"] == "ocr_fix"
    assert hit["matched_text"] == "glutan"
