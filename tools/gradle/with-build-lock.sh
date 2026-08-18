#!/usr/bin/env bash
# Global build serializer for local Gradle invocations, routed to from the
# tracked `gradlew` wrapper. Ensures at most one Gradle build runs at a time
# across every worktree/window on this machine, so concurrent Claude Code
# sessions don't corrupt the shared ~/.gradle build cache / transforms / .tmp
# staging dir or oversubscribe RAM/CPU.
#
# Usage (from gradlew):  with-build-lock.sh <JAVACMD> <java args...>
#
# Runs the build UNLOCKED (no serialization) when any of these hold, so it is
# inert on CI and can never break a build:
#   * BUILD_NUMBER set   — Jenkins (the only Gradle CI here)
#   * CI set             — any other CI runner
#   * MEGA_BUILD_LOCK=off — session-level bypass (e.g. `--continuous` builds)
#   * python3 unavailable
#
# See tools/gradle/README.md for the full contract and tunables.

set -uo pipefail

if [[ -n "${BUILD_NUMBER:-}" || -n "${CI:-}" || "${MEGA_BUILD_LOCK:-}" == "off" ]] \
    || ! command -v python3 >/dev/null 2>&1; then
    exec "$@"
fi

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec python3 "$HERE/build_lock.py" "$@"
