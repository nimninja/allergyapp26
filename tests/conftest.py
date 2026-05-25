"""Shared pytest fixtures."""

from __future__ import annotations

import os
from pathlib import Path

import pytest

# Before API modules read production defaults in tests.
os.environ.setdefault("RATE_LIMIT_ENABLED", "false")
os.environ.setdefault("ENV", "testing")

from src.match import load_default_rules_dir


@pytest.fixture
def rules_dir() -> Path:
    return load_default_rules_dir()
