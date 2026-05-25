# Ingredient Checker — production MVP

Scan food label photos against allergen and diet rules. Includes a **deployable API**, **mobile web UI**, **Android app**, and optional **desktop GUI**.

## Architecture

```
Android app (Kotlin)  ──HTTPS──▶  FastAPI (/v1/*)  ──▶  Tesseract OCR + YAML rules
Mobile web (/)        ──same API──┘
Desktop (app.py)      ──local OCR──▶  shared scan_service
```

## Quick start (local)

```powershell
pip install -r requirements.txt
uvicorn api.main:app --reload --host 0.0.0.0 --port 8000
```

- Mobile web: `http://YOUR_PC_IP:8000` (same Wi‑Fi)
- API docs (dev only): http://localhost:8000/docs
- Health: http://localhost:8000/health

## Production API (Docker)

```powershell
docker build -t ingredient-checker-api .
docker run -p 8000:8000 -e ENV=production ingredient-checker-api
```

### Deploy to Render

1. Push this repo to GitHub
2. [Render](https://render.com) → **New Web Service** → connect repo
3. Render detects `render.yaml` (Docker, health check on `/health`)
4. Note your URL, e.g. `https://ingredient-checker-api.onrender.com`

### Environment variables (production)

| Variable | Default | Purpose |
|----------|---------|---------|
| `OCR_ENGINE` | `tesseract` in Docker | `tesseract` (~512 MB RAM) or `easyocr` (~2 GB) |
| `ENV` | `development` | Set `production` to hide `/docs` and generic 500 errors |
| `RATE_LIMIT_ENABLED` | `true` in prod | Per-IP limits on `/v1/scan` |
| `RATE_LIMIT_SCAN` | `30/minute` | Scan rate limit |
| `MAX_UPLOAD_MB` | `10` | Max image size |
| `ALLOWED_ORIGINS` | `*` | CORS for web clients |
| `LOG_LEVEL` | `INFO` | Request logging |

## API (v1)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/health` | Load balancer probe |
| GET | `/v1/health` | Versioned health |
| GET | `/v1/allergens` | Allergen list + disclaimer |
| POST | `/v1/scan` | Multipart: `image`, `allergens`, `vegan`, `vegetarian`, `extra_avoid` |

Images are processed in memory and not stored.

## Android app (Play Store path)

1. Install [Android Studio](https://developer.android.com/studio)
2. **Open** the `android/` folder
3. Edit release API URL in `android/app/build.gradle.kts`:

   ```kotlin
   buildConfigField("String", "API_BASE_URL", "\"https://YOUR-RENDER-URL.onrender.com\"")
   ```

4. Run on emulator (debug uses `http://10.0.2.2:8000` → your local API)
5. **Build → Generate Signed Bundle** for Play Console

### Play Store checklist

- [ ] Deploy API with **HTTPS**
- [ ] Set release `API_BASE_URL` in Gradle
- [ ] Host a **privacy policy** (camera, photos sent to server for OCR)
- [ ] Complete Play **Data safety** form
- [ ] In-app **medical disclaimer** (returned by API)
- [ ] Internal testing track → production rollout

## Desktop app

```powershell
python app.py
```

## Tests

```powershell
python -m pytest -q
```

## Project layout

```
api/           FastAPI app, Docker entrypoint
src/           OCR, rules engine, scan pipeline
data/rules/    YAML allergen/diet keywords
android/       Kotlin + Compose store client
app.py         PySide6 desktop UI
```
