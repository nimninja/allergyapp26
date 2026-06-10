# Features roadmap

Ideas to add after Phase 2 (on-device ML Kit OCR + local rules). Not committed to order — pick by user impact and effort.

## Current (v2)

- Label photo → on-device OCR → keyword rules (allergens, vegan, vegetarian, custom avoid)
- Saved restrictions on device (DataStore)
- Android native app; Python desktop + Render API for web/legacy

---

## Planned features

### 1. Multi-language label scanning

**Goal:** Read ingredient lists in languages other than English (e.g. French, Spanish, German, Hindi).

**Why:** Many products sold globally; OCR and matching must work on non-Latin scripts and translated allergen terms.

**Possible approach:**

- ML Kit: add script-specific recognizers (Latin, Chinese, Devanagari, Japanese, Korean, etc.) and auto-detect or let user pick language
- Extend `data/rules/` with per-locale keyword lists (or `allergens.fr.yaml`, etc.)
- Normalize Unicode / diacritics before matching
- UI: “Label language” dropdown; fallback to multi-script attempt if unsure

**Open questions:** One model per language vs. unified pipeline; offline-only vs. optional cloud OCR for rare scripts.

---

### 2. Predefined diet & lifestyle profiles

**Goal:** One-tap profiles beyond vegan / vegetarian — e.g. gluten-free, kosher, halal, low-FODMAP, nut-free household, kid-safe pack lunch.

**Why:** Most users think in diets, not raw allergen IDs.

**Possible approach:**

- New YAML bundles: `profiles/gluten_free.yaml`, `profiles/halal.yaml`, … each maps to allergen selections + extra keywords + ambiguous warnings
- UI: profile chips (“Vegan”, “Gluten-free”, “Custom…”) that pre-fill restrictions; user can still override individual allergens
- Optional: import community-maintained profile packs

**Open questions:** Overlap when multiple profiles selected; medical vs. preference labeling in UI copy.

---

### 3. Barcode scan + product label (hybrid)

**Goal:** Scan UPC/EAN barcode first for known product metadata; still support photo of label when barcode unknown or packaging differs by region.

**Why:** Barcodes can jump straight to ingredient lists (Open Food Facts, retailer APIs); label OCR remains fallback and verification.

**Possible approach:**

- Android: ML Kit Barcode Scanning or ZXing
- Lookup: Open Food Facts API (free), optional commercial DB for coverage
- Flow: barcode → show cached ingredients → optional “Verify with photo” using current OCR path
- Offline: cache recent barcode lookups locally

**Open questions:** Privacy (barcode lookups leak shopping habits); handling stale or wrong OFF data; regional SKU differences.

---

### 4. User profiles with preferences

**Goal:** Named profiles (e.g. “Alex — peanut allergy”, “House — vegan”) with saved restrictions, default profile, and quick switch.

**Why:** Families and caregivers scan for different people; single global checkbox list is awkward.

**Possible approach:**

- Data model: `Profile { id, name, avatar?, restrictions, createdAt }`
- DataStore or Room for multiple profiles; “active profile” on home screen
- Optional sync: account + cloud backup (Firebase Auth, Supabase, or your Render API)
- Export/import profiles as JSON for backup

**Open questions:** Local-only vs. sign-in required; GDPR / COPPA if profiles include child names.

---

### 5. Smarter matching (LLM-assisted)

**Goal:** Tolerate OCR typos, ingredient synonyms, and vague wording (“natural flavors”, “spices”) without drowning in false positives.

**Why:** Keyword rules miss `whey protien` and over-flag harmless terms; users need nuance keyword lists cannot capture.

**Possible approach:**

- **On-device (limited):** fuzzy string match (Levenshtein), synonym table in YAML, phonetic hints for common OCR errors
- **LLM (cloud, opt-in):** send OCR text + user restrictions to a small model (GPT-4o-mini, Claude Haiku, Gemini Flash) with a strict JSON schema: `{ violations[], warnings[], confidence, explanation }`
- **Hybrid:** rules engine for hard hits; LLM only for ambiguous lines or low-confidence OCR blocks
- Always show raw OCR + rule hits; LLM output labeled “AI suggestion — verify packaging”

**Open questions:** Cost, latency, offline requirement, HIPAA/medical disclaimer, sending label text to third parties (privacy policy).

---

## Other ideas (backlog)

| Feature | Notes |
|--------|--------|
| Scan history | Timestamped results, re-open past scans, delete all |
| Share result | Export PDF/image summary for school/camp forms |
| Widget / shortcut | Quick scan from home screen |
| Apple iOS app | Same local OCR + rules architecture |
| Desktop sync | Sync profiles/rules via same account |
| “May contain” severity | Separate advisory vs. ingredient-list violations |
| Restaurant mode | Menu photo, no barcode |
| Community rules | Submit keyword suggestions; moderated merge |
| Accessibility | TalkBack, large text, high contrast |
| Play Store polish | Onboarding, privacy policy, crash reporting |

---

## Suggested phases

| Phase | Focus |
|-------|--------|
| **A** | User profiles (local), predefined profiles (YAML), scan history |
| **B** | Barcode + Open Food Facts; cache offline |
| **C** | Multi-language OCR + localized rules |
| **D** | LLM assist (opt-in cloud); fuzzy/synonym layer on-device first |

---

## How to extend this doc

Add items under **Planned features** or **Backlog** with: goal, why, possible approach, open questions. Link PRs/issues when work starts.
