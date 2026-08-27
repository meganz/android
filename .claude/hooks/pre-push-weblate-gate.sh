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

# Only gate `git push` (tolerating global flags, e.g. `git -C <dir> push`).
# Strip quoted segments first so a command that merely MENTIONS "git push"
# inside a string (echo, jq payloads, commit messages) is not gated.
STRIPPED=$(echo "$COMMAND" | sed -E "s/'[^']*'//g; s/\"[^\"]*\"//g")
echo "$STRIPPED" | grep -qE '\bgit(\s+-[A-Za-z-]+(\s+\S+)?)*\s+push\b' || exit 0

# Deletions and tag pushes don't publish branch content
echo "$STRIPPED" | grep -qE '\bpush\b[^|;&]*(--delete\b|--tags\b|\s+:\S)' && exit 0

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

# The hook payload's cwd is the SESSION directory, not the directory an
# inline `cd <dir> &&` or `git -C <dir>` actually runs the push in. Without
# resolving that, a worktree push gets gated against whatever branch the main
# checkout happens to have checked out.
EFFECTIVE_CWD="$CWD"
CD_RE='^[[:space:]]*cd[[:space:]]+("([^"]+)"|([^[:space:];&|]+))[[:space:]]*&&'
if [[ "$COMMAND" =~ $CD_RE ]]; then
    dir="${BASH_REMATCH[2]:-${BASH_REMATCH[3]}}"
    [[ "$dir" != /* ]] && dir="$CWD/$dir"
    [[ -d "$dir" ]] && EFFECTIVE_CWD="$dir"
fi
GITC_RE='git[[:space:]]+-C[[:space:]]+("([^"]+)"|([^[:space:]]+))'
if [[ "$COMMAND" =~ $GITC_RE ]]; then
    dir="${BASH_REMATCH[2]:-${BASH_REMATCH[3]}}"
    [[ "$dir" != /* ]] && dir="$EFFECTIVE_CWD/$dir"
    [[ -d "$dir" ]] && EFFECTIVE_CWD="$dir"
fi

# Prefer the explicitly pushed refspec (`git push -u origin <branch> …`) over
# the checked-out branch. Tokens that aren't a real local ref (e.g. words from
# a quoted push-option value) are discarded by the rev-parse check below.
pushed_ref=""
remote=""
seen_push=0
skip_val=0
read -r -a toks <<< "$COMMAND"
for tok in "${toks[@]}"; do
    if [[ $seen_push == 0 ]]; then
        [[ "$tok" == push ]] && seen_push=1
        continue
    fi
    [[ "$tok" == "&&" || "$tok" == ";" || "$tok" == "|" || "$tok" == "||" ]] && break
    if [[ $skip_val == 1 ]]; then
        skip_val=0
        continue
    fi
    case "$tok" in
        -o|--push-option|--repo) skip_val=1 ;;
        -*) ;;
        *[\<\>]*) ;;
        *)
            if [[ -z "$remote" ]]; then
                remote="$tok"
            else
                pushed_ref="$tok"
                break
            fi
            ;;
    esac
done
pushed_ref="${pushed_ref#refs/heads/}"
pushed_ref="${pushed_ref%%:*}"

BRANCH=""
if [[ -n "$pushed_ref" ]] && git -C "$EFFECTIVE_CWD" rev-parse --verify -q "refs/heads/$pushed_ref" >/dev/null 2>&1; then
    BRANCH="$pushed_ref"
fi
[[ -z "$BRANCH" ]] && BRANCH=$(git -C "$EFFECTIVE_CWD" branch --show-current 2>/dev/null || true)
[[ -z "$BRANCH" ]] && exit 0

TOPLEVEL=$(git -C "$EFFECTIVE_CWD" rev-parse --show-toplevel 2>/dev/null || true)
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

OUTPUT=$(cd "$EFFECTIVE_CWD" && WEBLATE_TOKEN="$TOKEN" bash "$GATE" "$BRANCH" origin/develop 2>&1)
RC=$?

BYPASS_NOTE="If this block is wrong (tooling bug, emergency), the user can bypass the gate by exporting WEBLATE_PUSH_GATE=off in their shell and restarting Claude Code (an inline VAR=off prefix on the push command has no effect) — the CI Weblate Strings Check will still verify the MR."

case "$RC" in
    0) exit 0 ;;
    1) deny "Weblate gate: $OUTPUT
$BYPASS_NOTE" ;;
    3) ask "Weblate gate: $OUTPUT — approve only if that is expected (stacked MR whose parent uploaded them); otherwise run /mega:weblate first." ;;
    *) ask "Weblate gate could not verify this push: $OUTPUT — see tools/weblate/README.md for setup, and confirm /mega:weblate has been run before approving." ;;
esac
