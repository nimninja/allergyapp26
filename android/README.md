# Ingredient Checker — Android

Native app with **on-device OCR (ML Kit)** and **bundled YAML rules** — works offline, no Render needed for scanning.

## Phase 2 (current)

```text
Camera / Gallery → ML Kit OCR (on phone) → local rules (assets/rules/) → results
Diet profile chips (Jain, Satvik, gluten-free, …) with expandable customization
```

- No internet required to scan labels
- Better OCR than server-side Tesseract
- Same rule files as `data/rules/` in the Python project

## Run the app

1. [Android Studio](https://developer.android.com/studio) → **Open** → `allergyappcursortest/android`
2. Gradle sync → connect phone (USB debugging) → **Run**

## Sync rules from Python project

When you change YAML in `data/rules/`, copy to Android assets:

```powershell
Copy-Item data\rules\* android\app\src\main\assets\rules\ -Force
```

Includes `profiles.yaml` for diet profile chips.

Then rebuild the app.

## Phase 1 (legacy)

The app previously uploaded images to `https://allergyapp26.onrender.com`. Phase 2 removed that dependency for scans. Retrofit/ApiClient remain in the repo for optional future use.

## Troubleshooting

| Problem | Fix |
|---------|-----|
| Run greyed out | Open `android/` folder; Gradle sync; Run → Edit Configurations → Android App → module **app** |
| No text from OCR | Better lighting; hold phone steady; try Gallery with a sharp photo |
| Gradle sync fails | SDK Manager → Android 15 (API 35) |

## Play Store

**Build → Generate Signed App Bundle (AAB)** when ready.

Privacy note: Phase 2 scans stay on device; state that in your privacy policy.
