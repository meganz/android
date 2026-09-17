#!/usr/bin/env bash
# Verifies that every commit a merge request adds on top of its target branch
# carries a GPG signature.
#
# Used by:
#   - the Jenkins MR pipeline (jenkinsfile/android_build_status.groovy,
#     stage "Commit Signature Check")
#
# Usage: commit_signature_gate.sh [<base-ref>] [<head-ref>]
#   <base-ref>  target branch to compare against, default origin/develop
#   <head-ref>  tip of the change, default HEAD
#
# Must be run from inside the android repo checkout (any directory).
#
# Environment:
#   SIGNATURE_GATE_FETCH  set to 1 to git-fetch the base ref before diffing (CI)
#
# Exit codes:
#   0  pass — no commits in the range, or all of them are signed
#   1  violation — at least one commit in the range has no signature
#   2  cannot verify — a ref could not be resolved
#
# Only the merge request's own range is inspected. develop carries unsigned
# commits from before this gate existed, so scanning full history would fail
# every merge request.
#
# A signature CI cannot validate still counts as signed: the agents hold no
# contributor public keys, so a genuinely signed commit reports %G? = E
# ("cannot check") rather than G. The assertion is therefore "not N" (no
# signature) — asserting "== G" would fail every merge request.
#
# %G? values: G good, B bad, U good/unknown validity, X good/expired,
# Y good/expired key, R good/revoked key, E cannot check, N no signature.

set -uo pipefail

BASE_REF="${1:-origin/develop}"
HEAD_REF="${2:-HEAD}"
RULE="----------------------------------------------------------------------"

if [[ "${SIGNATURE_GATE_FETCH:-}" == "1" ]]; then
    base_branch="${BASE_REF#origin/}"
    git fetch -q --no-tags origin "+refs/heads/${base_branch}:refs/remotes/origin/${base_branch}" || true
fi

for ref in "$BASE_REF" "$HEAD_REF"; do
    if ! git rev-parse --verify -q "$ref" >/dev/null; then
        echo "Cannot verify: ref '$ref' not found in this checkout."
        exit 2
    fi
done

COMMITS=$(git log --no-merges --format='%G?|%h|%s' "$BASE_REF..$HEAD_REF")
if [[ -z "$COMMITS" ]]; then
    echo "No commits in $BASE_REF..$HEAD_REF — nothing to verify."
    exit 0
fi

TOTAL=$(echo "$COMMITS" | wc -l | tr -d ' ')
UNSIGNED=$(echo "$COMMITS" | grep '^N|' | sed -E 's/^N\|([^|]*)\|/  \1  /')

if [[ -z "$UNSIGNED" ]]; then
    echo "OK — all $TOTAL commit(s) in $BASE_REF..$HEAD_REF are signed."
    exit 0
fi

COUNT=$(echo "$UNSIGNED" | wc -l | tr -d ' ')

cat <<EOF
UNSIGNED COMMITS — $COUNT of the $TOTAL commit(s) this merge request adds on
top of $BASE_REF carry no GPG signature:

$UNSIGNED

$RULE
A. Fix this branch now
$RULE
Re-create the commits so that they are signed, then update the remote
branch:

    git fetch origin
    git rebase --force-rebase --gpg-sign $BASE_REF
    git push --force-with-lease

The rebase replays the same commits with the same contents and the same
messages, so the change set under review is unaffected. Only the commit
SHAs change, and that is why the branch has to be rewritten on the
remote rather than pushed to normally.

Use --force-with-lease, not --force. The lease makes the push refuse to
run if anyone else has pushed to the branch since your last fetch, so a
rewrite cannot silently discard someone else's work. A plain --force
overwrites whatever is there.

$RULE
B. Stop this happening again
$RULE
    git config --global commit.gpgsign true
    git config --global rebase.gpgSign true

commit.gpgsign is the setting that actually matters. On current git it
is honoured by commit, amend, cherry-pick, revert, merge and rebase
alike — verified on git 2.39.1 with both rebase backends. rebase.gpgSign
is belt-and-braces: it adds nothing on top of commit.gpgsign, but it
states the intent explicitly.

For a one-off, git cherry-pick and git am both accept -S.

If commits keep losing their signature even with the config set, check
that signing works in a non-interactive shell. A gpg-agent that cannot
prompt for a passphrase there is the usual cause: the signing attempt
fails, the commit is still created, and tooling and automation carry on
without anyone seeing the failure.
EOF

exit 1
