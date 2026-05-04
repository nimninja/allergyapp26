"""Normalize label text and match against YAML keyword rules."""

from __future__ import annotations

import re
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

import yaml


@dataclass
class MatchResult:
    raw_text: str
    normalized: str
    violations: list[dict[str, str]] = field(default_factory=list)
    warnings: list[dict[str, str]] = field(default_factory=list)
    may_contain_notices: list[str] = field(default_factory=list)


def project_root() -> Path:
    return Path(__file__).resolve().parent.parent


def load_default_rules_dir() -> Path:
    return project_root() / "data" / "rules"


def normalize_text(text: str) -> str:
    if not text:
        return ""
    lower = text.lower()
    # Keep letters, numbers, spaces; turn other punctuation into spaces
    cleaned = re.sub(r"[^a-z0-9\s\-/]", " ", lower)
    cleaned = re.sub(r"\s+", " ", cleaned)
    return cleaned.strip()


def _load_yaml(path: Path) -> dict[str, Any]:
    with path.open(encoding="utf-8") as f:
        return yaml.safe_load(f) or {}


def _find_keyword_in_normalized(normalized: str, keyword: str) -> bool:
    kw = keyword.strip().lower()
    if not kw:
        return False
    # Multi-word phrases: substring search on normalized text
    if " " in kw:
        norm_kw = normalize_text(kw)
        return norm_kw in normalized if norm_kw else False
    # Single token / token-like: word boundary to reduce false positives
    try:
        return re.search(rf"\b{re.escape(kw)}\b", normalized) is not None
    except re.error:
        return kw in normalized


def _collect_may_contain(original_text: str) -> list[str]:
    lines: list[str] = []
    for raw_line in original_text.splitlines():
        line = raw_line.strip()
        low = line.lower()
        if any(
            phrase in low
            for phrase in (
                "may contain",
                "may also contain",
                "processed in a facility",
                "processed on equipment",
                "manufactured in a facility",
                "made in a facility",
                "shared equipment",
            )
        ):
            lines.append(line)
    return lines


def match_text(
    raw_text: str,
    *,
    rules_dir: Path,
    selected_allergens: list[str],
    vegan: bool,
    vegetarian: bool,
    extra_avoid: list[str],
) -> MatchResult:
    normalized = normalize_text(raw_text)
    violations: list[dict[str, str]] = []
    warnings: list[dict[str, str]] = []
    may_contain_notices = _collect_may_contain(raw_text)

    allergens_data = _load_yaml(rules_dir / "allergens.yaml")
    vegan_data = _load_yaml(rules_dir / "vegan.yaml")
    vegetarian_data = _load_yaml(rules_dir / "vegetarian.yaml")
    ambiguous_data = _load_yaml(rules_dir / "ambiguous.yaml")

    seen_pairs: set[tuple[str, str]] = set()
    seen_warning_terms: set[str] = set()

    def add_violation(category: str, keyword: str) -> None:
        key = (category, keyword)
        if key in seen_pairs:
            return
        seen_pairs.add(key)
        violations.append({"category": category, "keyword": keyword})

    # Allergens
    for allergen_id in selected_allergens:
        entry = allergens_data.get(allergen_id)
        if not entry:
            continue
        label = entry.get("label", allergen_id)
        for kw in entry.get("keywords", []):
            if isinstance(kw, str) and _find_keyword_in_normalized(normalized, kw):
                add_violation(f"Allergen: {label}", kw)

    # Vegan
    if vegan:
        for kw in vegan_data.get("violation_keywords", []):
            if isinstance(kw, str) and _find_keyword_in_normalized(normalized, kw):
                add_violation("Vegan", kw)
        for item in ambiguous_data.get("vegan_ambiguous", []):
            if isinstance(item, dict):
                term = item.get("term", "")
                reason = item.get("reason", "Ambiguous on labels; verify source.")
            else:
                term, reason = str(item), "Ambiguous on labels; verify source."
            if term and _find_keyword_in_normalized(normalized, term):
                if term not in seen_warning_terms:
                    seen_warning_terms.add(term)
                    warnings.append({"term": term, "reason": reason})

    # Vegetarian
    if vegetarian:
        for kw in vegetarian_data.get("violation_keywords", []):
            if isinstance(kw, str) and _find_keyword_in_normalized(normalized, kw):
                add_violation("Vegetarian", kw)
        for item in ambiguous_data.get("vegetarian_ambiguous", []):
            if isinstance(item, dict):
                term = item.get("term", "")
                reason = item.get("reason", "May or may not be animal-derived.")
            else:
                term, reason = str(item), "May or may not be animal-derived."
            if term and _find_keyword_in_normalized(normalized, term):
                if term not in seen_warning_terms:
                    seen_warning_terms.add(term)
                    warnings.append({"term": term, "reason": reason})

    # User-supplied terms (treat as violations when matched)
    for term in extra_avoid:
        if _find_keyword_in_normalized(normalized, term):
            add_violation(f"Custom avoid: {term}", term)

    return MatchResult(
        raw_text=raw_text,
        normalized=normalized,
        violations=violations,
        warnings=warnings,
        may_contain_notices=may_contain_notices,
    )
