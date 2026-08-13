#!/usr/bin/env bash
# Device-access gate for Claude Code (PreToolUse / Bash).
#
# Thin adapter to `tools/device/lease gate`, which reads the hook JSON on stdin
# and emits the allow/deny decision (or nothing, to defer). Mirrors the
# hook -> shared tools/<x> script pattern of pre-push-weblate-gate.sh. The gate:
#   * DENY a flavored connected instrumented test with no ANDROID_SERIAL
#     (it would fan out to every attached device and collide);
#   * DENY an ANDROID_SERIAL / `adb -s` targeting a device leased by another
#     git worktree;
#   * ALLOW (and heartbeat) a device this worktree already holds;
#   * defer everything else to the normal permission flow.
#
# Bypass (user-controlled session env, not the command string):
#   export MEGA_DEVICE_GATE=off

set -uo pipefail

command -v jq >/dev/null 2>&1 || exit 0
command -v python3 >/dev/null 2>&1 || exit 0
[[ "${MEGA_DEVICE_GATE:-}" == "off" ]] && exit 0

INPUT=$(cat)
CWD=$(printf '%s' "$INPUT" | jq -r '.cwd // empty')

# Resolve the worktree's own lease.py (git toplevel of cwd first, then the
# session project dir) so all worktrees hit the same shared lease store.
LEASE=""
for dir in "$CWD" "${CLAUDE_PROJECT_DIR:-}"; do
    [[ -z "$dir" ]] && continue
    top=$(git -C "$dir" rev-parse --show-toplevel 2>/dev/null || true)
    for cand in "$top" "$dir"; do
        if [[ -n "$cand" && -f "$cand/tools/device/lease.py" ]]; then
            LEASE="$cand/tools/device/lease.py"
            break 2
        fi
    done
done
[[ -z "$LEASE" ]] && exit 0

printf '%s' "$INPUT" | python3 "$LEASE" gate
exit 0
