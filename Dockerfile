FROM python:3.12-slim-bookworm

WORKDIR /app

# Tesseract OCR (lightweight; fits Render Free/Starter 512 MB)
RUN apt-get update && apt-get install -y --no-install-recommends \
    tesseract-ocr \
    tesseract-ocr-eng \
    && rm -rf /var/lib/apt/lists/*

COPY requirements-api.txt .
RUN pip install --no-cache-dir -r requirements-api.txt

COPY api/ api/
COPY src/ src/
COPY data/ data/

ENV ENV=production \
    PORT=8000 \
    OCR_ENGINE=tesseract \
    RATE_LIMIT_ENABLED=true \
    PYTHONUNBUFFERED=1

EXPOSE 8000

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8000/health', timeout=5)"

CMD ["sh", "-c", "uvicorn api.main:app --host 0.0.0.0 --port ${PORT} --timeout-keep-alive 120 --workers 1"]
