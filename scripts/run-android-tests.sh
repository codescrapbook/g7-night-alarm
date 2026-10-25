#!/usr/bin/env bash
set -euo pipefail

# Run Android emulator tests in Docker with KVM. Requires:
# - Linux host with /dev/kvm available (or Windows 10 with WSL2 + Docker Desktop + nested virtualization enabled)
# - A self-hosted runner or local machine with the 'android-emulator' label (for CI)
#
# Usage:
#   ./scripts/run-android-tests.sh
#
# Artifacts: writes junit reports, screenshots, and logs under ./artifacts/,
# which CI maps to /opt/cursor/artifacts/test-reports/ in this environment.

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

docker compose -f "$ROOT_DIR/docker-compose.yml" build android-emulator
docker compose -f "$ROOT_DIR/docker-compose.yml" run --rm --device /dev/kvm android-emulator

