"""Shared constants for desktop and API clients."""

ALLERGEN_OPTIONS: list[tuple[str, str]] = [
    ("milk", "Milk / dairy"),
    ("eggs", "Eggs"),
    ("fish", "Fish"),
    ("shellfish", "Shellfish (crustacean)"),
    ("tree_nuts", "Tree nuts"),
    ("peanuts", "Peanuts"),
    ("wheat", "Wheat / gluten (wheat)"),
    ("soy", "Soy"),
    ("sesame", "Sesame"),
]

ALLERGEN_IDS = frozenset(key for key, _ in ALLERGEN_OPTIONS)

DISCLAIMER = (
    "This tool is not medical advice. OCR can misread labels; ingredients vary by region. "
    "Always read the actual packaging. Cross-contact (“may contain”) is not fully detectable "
    "from ingredients alone."
)
