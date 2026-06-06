#!/usr/bin/env bash
# Run the backend locally for device testing.
# Usage: ./run_local.sh [mode]
# mode: "test" to force test-login, "google" to use Google token validation (default: test)

set -euo pipefail
MODE=${1:-test}
export TEST_SIGNIN_MODE=${MODE}

# Run uvicorn from repo root: module path is backend.main:app
exec uvicorn backend.main:app --host 0.0.0.0 --port 8000 --reload
