#!/usr/bin/env bash
# Verifies that new/changed strings in strings_shared.xml on <branch> exist in
# the branch's Weblate component (strings_shared-<sanitized-branch>) with a
# description and a mapped screenshot.
#
# Shared by:
#   - the Claude Code pre-push hook (.claude/hooks/pre-push-weblate-gate.sh)
#   - the Jenkins MR pipeline (jenkinsfile/android_build_status.groovy,
#     stage "Weblate Strings Check")
#
# Usage: weblate_gate.sh <branch> [<base-ref>]
#   <branch>    source branch of the change (an "origin/" prefix is stripped)
#   <base-ref>  diff base, default origin/develop
#
# Must be run from inside the android repo checkout (any directory).
#
# Environment:
#   WEBLATE_TOKEN       (required) Weblate API token
#   WEBLATE_BASE_URL    default https://translate.developers.mega.co.nz/api
#   WEBLATE_GATE_FETCH  set to 1 to git-fetch the base ref before diffing (CI)
#
# Exit codes:
#   0  pass — no string changes, exempt branch, or everything verified
#   1  violation — strings not uploaded, missing description, or missing
#      screenshot
#   2  cannot verify — network or configuration problem
#   3  soft finding only — strings exist in another Weblate component (stacked
#      MR parent, or a change to an already-released string); caller decides

set -uo pipefail

BRANCH="${1:-}"
BASE_REF="${2:-origin/develop}"
STRINGS_FILE="resources/string-resources/src/main/res/values/strings_shared.xml"

if [[ -z "$BRANCH" ]]; then
    echo "usage: weblate_gate.sh <branch> [<base-ref>]"
    exit 2
fi
BRANCH="${BRANCH#origin/}"

case "$BRANCH" in
    develop|master|main|release/*|task/pre-release/*)
        echo "Branch '$BRANCH' is exempt from the Weblate gate."
        exit 0
        ;;
esac

for tool in jq curl git; do
    if ! command -v "$tool" &>/dev/null; then
        echo "Cannot verify: required tool '$tool' is not installed."
        exit 2
    fi
done

if [[ "${WEBLATE_GATE_FETCH:-}" == "1" ]]; then
    base_branch="${BASE_REF#origin/}"
    git fetch -q --no-tags origin "+refs/heads/${base_branch}:refs/remotes/origin/${base_branch}" || true
fi

if ! git rev-parse --verify -q "$BASE_REF" >/dev/null; then
    echo "Cannot verify: base ref '$BASE_REF' not found in this checkout."
    exit 2
fi

NEW_NAMES=$(git diff "$BASE_REF"...HEAD -- "$STRINGS_FILE" 2>/dev/null \
    | grep -E '^\+' \
    | grep -oE '<(string|plurals) name="[^"]+"' \
    | sed -E 's/.*name="([^"]+)"/\1/' \
    | sort -u)
if [[ -z "$NEW_NAMES" ]]; then
    echo "No added/changed strings in $STRINGS_FILE vs $BASE_REF — nothing to verify."
    exit 0
fi

BASE="${WEBLATE_BASE_URL:-https://translate.developers.mega.co.nz/api}"
TOKEN="${WEBLATE_TOKEN:-}"
if [[ -z "$TOKEN" ]]; then
    echo "Cannot verify: WEBLATE_TOKEN is not set."
    exit 2
fi

SLUG=$(echo "$BRANCH" | sed 's/[^A-Za-z0-9]//g' | tr '[:upper:]' '[:lower:]')
COMP="strings_shared-${SLUG}"

TMP=$(mktemp)
trap 'rm -f "$TMP"' EXIT

api_get() {
    curl -s --max-time 15 -o "$TMP" -w '%{http_code}' \
        -H "Authorization: Token $TOKEN" "$1" 2>/dev/null || echo "000"
}

network_fail() {
    echo "Cannot verify: Weblate API request failed (HTTP $1) for $2."
    exit 2
}

STATUS=$(api_get "$BASE/components/android/$COMP/")
if [[ "$STATUS" == "200" ]]; then
    COMP_EXISTS=1
elif [[ "$STATUS" == "404" ]]; then
    # Not necessarily a violation yet: a stacked MR's strings live under the
    # parent branch's component — classify per string below before deciding.
    COMP_EXISTS=0
else
    network_fail "$STATUS" "component lookup"
fi

if [[ "$COMP_EXISTS" == 1 ]]; then
    STATUS=$(api_get "$BASE/translations/android/$COMP/en/units/?page_size=500")
    [[ "$STATUS" != "200" ]] && network_fail "$STATUS" "unit listing"
    UNITS_JSON=$(cat "$TMP")
else
    UNITS_JSON='{"results":[]}'
fi

missing_units=""
missing_desc=""
name_id_pairs=""
for name in $NEW_NAMES; do
    unit=$(echo "$UNITS_JSON" | jq -c --arg n "$name" '[.results[] | select(.context == $n)][0] // empty')
    if [[ -z "$unit" ]]; then
        missing_units+=" $name"
        continue
    fi
    desc=$(echo "$unit" | jq -r '(.explanation // "") + (.note // "")' | tr -d '[:space:]')
    [[ -z "$desc" ]] && missing_desc+=" $name"
    name_id_pairs+="$name=$(echo "$unit" | jq -r '.id') "
done

# Strings absent from the branch component may still exist in another
# component (stacked MR whose parent uploaded them, or a change to an
# already-released string) — those are a soft finding, not a violation.
truly_missing=""
elsewhere=""
for name in $missing_units; do
    STATUS=$(api_get "$BASE/units/?q=context:%3D$name%20and%20project:android%20and%20language:en&page_size=1")
    [[ "$STATUS" != "200" ]] && network_fail "$STATUS" "global unit search"
    count=$(jq -r '.count // 0' "$TMP")
    if [[ "$count" == "0" || "$count" == "null" ]]; then
        truly_missing+=" $name"
    else
        elsewhere+=" $name"
    fi
done

missing_shot=""
if [[ "$COMP_EXISTS" == 1 && -n "$name_id_pairs" ]]; then
    STATUS=$(api_get "$BASE/components/android/$COMP/screenshots/")
    if [[ "$STATUS" == "200" ]]; then
        mapped_ids=$(jq -r '.results[].units[]' "$TMP" 2>/dev/null | sed -E 's|.*/units/([0-9]+)/?$|\1|' | sort -u)
        for pair in $name_id_pairs; do
            name="${pair%%=*}"
            uid="${pair##*=}"
            echo "$mapped_ids" | grep -qx "$uid" || missing_shot+=" $name"
        done
    else
        network_fail "$STATUS" "screenshot listing"
    fi
fi

violations=0
if [[ -n "$truly_missing" ]]; then
    echo "NOT UPLOADED — these strings are not in Weblate (expected component: $COMP):$truly_missing"
    violations=1
fi
if [[ -n "$missing_desc" ]]; then
    echo "NO DESCRIPTION — these strings are in $COMP but have neither a source note nor an explanation:$missing_desc"
    violations=1
fi
if [[ -n "$missing_shot" ]]; then
    echo "NO SCREENSHOT — these strings are in $COMP but have no screenshot mapped:$missing_shot"
    violations=1
fi
if [[ -n "$elsewhere" ]]; then
    echo "IN ANOTHER COMPONENT — these strings are not in $COMP but exist elsewhere in Weblate (stacked MR parent, or an already-released string):$elsewhere"
fi

if [[ "$violations" == 1 ]]; then
    echo "Run /weblate (see .claude/skills/weblate/SKILL.md) to upload strings with descriptions and screenshots."
    exit 1
fi

[[ -n "$elsewhere" ]] && exit 3

echo "OK — all $(echo "$NEW_NAMES" | wc -l | tr -d ' ') changed string(s) are in $COMP with descriptions and screenshots."
exit 0
