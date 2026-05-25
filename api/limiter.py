"""Shared rate limiter instance (must be imported by routes and main)."""

from __future__ import annotations

from slowapi import Limiter
from slowapi.util import get_remote_address

from api.config import settings

limiter = Limiter(
    key_func=get_remote_address,
    enabled=settings.rate_limit_enabled,
    default_limits=[],
)
