"""Runtime configuration from environment variables."""

from __future__ import annotations

import os
from dataclasses import dataclass


def _env_bool(name: str, default: bool) -> bool:
    raw = os.environ.get(name)
    if raw is None:
        return default
    return raw.strip().lower() in {"1", "true", "yes", "on"}


@dataclass(frozen=True)
class Settings:
    env: str
    is_production: bool
    max_upload_bytes: int
    allowed_origins: list[str]
    rate_limit_scan: str
    rate_limit_enabled: bool
    log_level: str

    @classmethod
    def from_env(cls) -> Settings:
        env = os.environ.get("ENV", "development").strip().lower()
        origins_raw = os.environ.get("ALLOWED_ORIGINS", "*").strip()
        if origins_raw == "*":
            allowed = ["*"]
        else:
            allowed = [o.strip() for o in origins_raw.split(",") if o.strip()]
        max_mb = int(os.environ.get("MAX_UPLOAD_MB", "10"))
        return cls(
            env=env,
            is_production=env == "production",
            max_upload_bytes=max_mb * 1024 * 1024,
            allowed_origins=allowed,
            rate_limit_scan=os.environ.get("RATE_LIMIT_SCAN", "30/minute"),
            rate_limit_enabled=_env_bool("RATE_LIMIT_ENABLED", env == "production"),
            log_level=os.environ.get("LOG_LEVEL", "INFO").upper(),
        )


settings = Settings.from_env()
