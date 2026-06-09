# Ingredient Checker — Android (Phase 1)

Native app that talks to your **live Render API** (`https://allergyapp26.onrender.com`).

Same flow as the mobile website: pick restrictions → photo → analyze → results.

## Prerequisites

1. [Android Studio](https://developer.android.com/studio) (latest stable)
2. Android phone with **USB debugging** enabled, or an emulator
3. Render service **live** at https://allergyapp26.onrender.com/health

## Open the project

1. Android Studio → **Open**
2. Select this folder: `allergyappcursortest/android` (not the repo root)
3. Wait for **Gradle sync** to finish (first time may download SDKs)

## Run on your phone

1. Phone: **Settings → Developer options → USB debugging** ON
2. Connect USB → allow debugging on phone
3. Android Studio toolbar: select your **device**
4. Click **Run** (green triangle)

The app installs and opens **Ingredient Checker**.

## Use the app

1. Allergen checkboxes load from the API (needs internet)
2. **Camera** or **Gallery** → pick a label photo
3. **Analyze label** — first scan after idle may take 30–40s (Render free tier)
4. Results show violations, warnings, OCR text

## API URL

Configured in `app/build.gradle.kts`:

```kotlin
val productionApiUrl = "https://allergyapp26.onrender.com"
```

Debug and release builds both use this URL (works on physical phone and emulator).

To point at a local API later (emulator only): use `http://10.0.2.2:8000` in the `debug` block.

## Troubleshooting

| Problem | Fix |
|---------|-----|
| **Run button greyed out** | See below |
| "Could not reach API" | Check phone has internet; open Render URL in Chrome |
| Scan times out | Wait 30s, retry (cold start); try Wi‑Fi |
| Gradle sync fails | Install SDK 35 via SDK Manager |
| No device shown | Install USB driver (Windows) or use emulator |
| Camera denied | App settings → allow Camera permission |

### Run button greyed out

1. **Open the correct folder** — must be `allergyappcursortest/android`, not the repo root.
2. Wait for **Gradle sync** to finish (bottom status bar). If it failed, open **Build** tab and read the error.
3. **Create a run configuration:**
   - **Run → Edit Configurations…**
   - **+** → **Android App**
   - Name: `app`
   - Module: **app** (if empty, sync failed — fix Gradle first)
   - **OK**
4. **Install SDK:** **Tools → SDK Manager** → SDK Platforms → **Android 15 (API 35)** → Apply.
5. **JDK:** **File → Settings → Build, Execution, Deployment → Build Tools → Gradle** → Gradle JDK = **Embedded JDK** (17 or 21).
6. **File → Sync Project with Gradle Files**, then pick your phone in the device dropdown.

If `JAVA_HOME` on your PC points at an `.msi` installer file, ignore it for Android Studio (use Embedded JDK above). Remove that bad `JAVA_HOME` from Windows env vars if command-line builds fail.

## Build release APK (optional)

**Build → Generate Signed App Bundle or APK** → follow wizard (create keystore first time).

For Play Store use **AAB** (App Bundle).

## Phase 2 (later)

On-device **ML Kit OCR** + rules in the app — no Render needed for scanning.
