#!/usr/bin/env bash
# Starts the packaged jar with a configurable port.
# Usage: PORT=8080 ./run.sh   (defaults to 8080 if PORT is unset)
set -euo pipefail
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PORT="${PORT:-8080}"
exec java -jar "$DIR/httpserver.jar" "$PORT"
