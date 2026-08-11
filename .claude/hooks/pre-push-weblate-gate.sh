#!/usr/bin/env bash
# Weblate push gate for Claude Code (PreToolUse / Bash).
#
# Blocks `git push` of a feature branch that adds or changes strings in
# strings_shared.xml until every one of those strings exists in the branch's
# Weblate component with a description and a mapped screenshot. The actual
# verification lives in tools/weblate/weblate_gate.sh, shared with the
# Jenkins MR pipeline ("Weblate Strings Check" stage) — this hook only
# adapts it to the Claude Code hook protocol:
#
#   gate exit 0 (pass/exempt/no strings)  -> fall through to normal permissions
#   gate exit 1 (violation)               -> deny
#   gate exit 2 (cannot verify)           -> ask, with setup pointer
#   gate exit 3 (strings in another
#                component: stacked MR)   -> ask
#
# Emergency bypass (user-controlled only — the session environment, not the
# command string, so an inline VAR=off prefix on the push command has no
# effect): export WEBLATE_PUSH_GATE=off before launching Claude Code.

set -uo pipefail

command -v jq &>/dev/null || exit 0
[[ "${WEBLATE_PUSH_GATE:-}" == "off" ]] && exit 0

INPUT=$(cat)
COMMAND=$(echo "$INPUT" | jq -r '.tool_input.command // empty')
CWD=$(echo "$INPUT" | jq -r '.cwd // empty')
[[ -z "$COMMAND" || -z "$CWD" ]] && exit 0

# Only gate `git push` (tolerating global flags, e.g. `git -C <dir> push`)
echo "$COMMAND" | grep -qE '\bgit(\s+-[A-Za-z-]+(\s+\S+)?)*\s+push\b' || exit 0

# Deletions and tag pushes don't publish branch content
echo "$COMMAND" | grep -qE '\bpush\b[^|;&]*(--delete\b|--tags\b|\s+:\S)' && exit 0

deny() {
    jq -n --arg reason "$1" '{
        hookSpecificOutput: {
            hookEventName: "PreToolUse",
            permissionDecision: "deny",
            permissionDecisionReason: $reason
        }
    }'
    exit 0
}

ask() {
    jq -n --arg reason "$1" '{
        hookSpecificOutput: {
            hookEventName: "PreToolUse",
            permissionDecision: "ask",
            permissionDecisionReason: $reason
        }
    }'
    exit 0
}

# Pushes publish the checked-out branch in this workflow (create-mr and the
# worktree push-option flow both push HEAD); parsing a refspec out of quoted
# push options is unreliable, so gate on the branch at the command's cwd.
BRANCH=$(git -C "$CWD" branch --show-current 2>/dev/null || true)
[[ -z "$BRANCH" ]] && exit 0

TOPLEVEL=$(git -C "$CWD" rev-parse --show-toplevel 2>/dev/null || true)
GATE=""
for dir in "$TOPLEVEL" "${CLAUDE_PROJECT_DIR:-}"; do
    if [[ -n "$dir" && -f "$dir/tools/weblate/weblate_gate.sh" ]]; then
        GATE="$dir/tools/weblate/weblate_gate.sh"
        break
    fi
done
[[ -z "$GATE" ]] && exit 0

# The Weblate token lives in the separate transifex checkout, which only
# exists in the main worktree — resolve from CLAUDE_PROJECT_DIR as fallback.
TOKEN=""
for dir in "$TOPLEVEL" "${CLAUDE_PROJECT_DIR:-}"; do
    if [[ -n "$dir" && -f "$dir/transifex/weblate/translate.json" ]]; then
        TOKEN=$(jq -r '.SOURCE_TOKEN // empty' "$dir/transifex/weblate/translate.json")
        break
    fi
done

OUTPUT=$(cd "$CWD" && WEBLATE_TOKEN="$TOKEN" bash "$GATE" "$BRANCH" origin/develop 2>&1)
RC=$?

BYPASS_NOTE="If this block is wrong (tooling bug, emergency), the user can bypass the gate by exporting WEBLATE_PUSH_GATE=off in their shell and restarting Claude Code (an inline VAR=off prefix on the push command has no effect) — the CI Weblate Strings Check will still verify the MR."

case "$RC" in
    0) exit 0 ;;
    1) deny "Weblate gate: $OUTPUT
$BYPASS_NOTE" ;;
    3) ask "Weblate gate: $OUTPUT — approve only if that is expected (stacked MR whose parent uploaded them); otherwise run /weblate first." ;;
    *) ask "Weblate gate could not verify this push: $OUTPUT — see tools/weblate/README.md for setup, and confirm /weblate has been run before approving." ;;
esac
