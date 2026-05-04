"""Streamlit entry: dietary restrictions, image OCR, and rule matching."""

from __future__ import annotations

import streamlit as st

from src.match import MatchResult, load_default_rules_dir, match_text
from src.ocr import extract_text_from_image, get_easyocr_reader

RULES_DIR = load_default_rules_dir()


@st.cache_resource
def _cached_ocr_reader():
    return get_easyocr_reader()

ALLERGEN_OPTIONS = [
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


def main() -> None:
    st.set_page_config(
        page_title="Ingredient checker",
        page_icon="🥗",
        layout="wide",
    )
    st.title("Allergy & dietary ingredient checker")
    st.caption(
        "Upload or photograph an ingredient label. Results use keyword rules and OCR—always verify on the package."
    )

    with st.sidebar:
        st.header("Your restrictions")
        selected_allergens = []
        for key, label in ALLERGEN_OPTIONS:
            if st.checkbox(label, key=f"allergen_{key}"):
                selected_allergens.append(key)
        vegan = st.checkbox("Vegan")
        vegetarian = st.checkbox("Vegetarian")
        extra_avoid = st.text_area(
            "Additional terms to avoid (one per line or comma-separated)",
            height=100,
            placeholder="e.g. coconut, mustard",
        )

    col_a, col_b = st.columns(2)
    with col_a:
        uploaded = st.file_uploader(
            "Upload label image",
            type=["png", "jpg", "jpeg", "webp"],
            accept_multiple_files=False,
        )
    with col_b:
        camera = st.camera_input("Take a photo")

    image_bytes_source = None
    if uploaded is not None:
        image_bytes_source = uploaded.getvalue()
        st.image(uploaded, caption="Uploaded image", use_container_width=True)
    elif camera is not None:
        image_bytes_source = camera.getvalue()
        st.image(camera, caption="Camera capture", use_container_width=True)

    analyze = st.button("Analyze label", type="primary", disabled=image_bytes_source is None)

    with st.expander("Disclaimer", expanded=False):
        st.markdown(
            """
This tool is **not medical advice**. OCR can misread labels; ingredients and
allergen statements vary by region. Always read the actual packaging and consult
a healthcare professional for severe allergies. Cross-contact (“may contain”) is
not fully detectable from ingredients alone.
            """
        )

    if analyze and image_bytes_source:
        with st.spinner("Reading text from image…"):
            raw_text = extract_text_from_image(
                image_bytes_source, reader=_cached_ocr_reader()
            )
        extra_terms = _parse_extra_avoid(extra_avoid)
        result: MatchResult = match_text(
            raw_text,
            rules_dir=RULES_DIR,
            selected_allergens=selected_allergens,
            vegan=vegan,
            vegetarian=vegetarian,
            extra_avoid=extra_terms,
        )
        _render_results(result)
    elif analyze:
        st.warning("Provide an image via upload or camera first.")


def _parse_extra_avoid(text: str) -> list[str]:
    if not text or not text.strip():
        return []
    parts: list[str] = []
    for line in text.replace(",", "\n").splitlines():
        t = line.strip()
        if t:
            parts.append(t)
    return parts


def _render_results(result: MatchResult) -> None:
    if not result.raw_text.strip():
        st.error("No text could be read from the image. Try better lighting, closer crop, or a sharper photo.")
        return

    st.subheader("Summary")
    if result.violations:
        st.error(f"**Potential violations ({len(result.violations)}):** restrictions you selected may not be met.")
    elif result.warnings:
        st.warning("No strong violations matched, but there are items to review below.")
    else:
        st.success("No keyword violations matched your selected restrictions (still verify the label yourself).")

    if result.may_contain_notices:
        st.warning("**May contain / facility wording detected:**")
        for line in result.may_contain_notices:
            st.write(f"- {line}")

    if result.violations:
        st.subheader("Violations")
        for v in result.violations:
            st.write(f"- **{v['category']}**: matched `{v['keyword']}`")

    if result.warnings:
        st.subheader("Review manually")
        for w in result.warnings:
            st.write(f"- **{w['term']}**: {w['reason']}")

    with st.expander("Extracted text (OCR)", expanded=True):
        st.text(result.raw_text)


if __name__ == "__main__":
    main()
