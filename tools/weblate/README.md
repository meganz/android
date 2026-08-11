# Weblate string gate

New or changed strings in
`resources/string-resources/src/main/res/values/strings_shared.xml` must be uploaded to the
branch's Weblate component (`strings_shared-<sanitized-branch>`) **with a description and a
mapped screenshot** before the branch is pushed / the MR is merged. The `/weblate` Claude Code
skill (`.claude/skills/weblate/SKILL.md`) performs the full upload flow.

`weblate_gate.sh` verifies this and is enforced in two places:

| Where | How | On violation |
|---|---|---|
| Claude Code | `.claude/hooks/pre-push-weblate-gate.sh` intercepts `git push` | Push is denied with the report |
| Jenkins MR pipeline | `jenkinsfile/android_build_status.groovy`, stage **Weblate Strings Check** | Pipeline fails, report posted as MR comment |

Exempt branches: `develop`, `master`, `main`, `release/*`, `task/pre-release/*` (release MRs).
Verification problems (network, missing token) never hard-block: the hook downgrades to a
confirmation prompt and the CI stage posts a warning comment instead of failing.

## Manual run

Locally:

```bash
WEBLATE_TOKEN=<token> bash tools/weblate/weblate_gate.sh "$(git branch --show-current)"
```

On an MR: comment **`weblate_check`** (like `jenkins rebuild` / `code_review`) to re-run only
the Weblate Strings Check stage; it replies with an explicit pass/fail comment.

Exit codes: `0` pass · `1` violation · `2` cannot verify · `3` strings found only in another
component (stacked MR parent / already-released string).

## Developer setup

The Weblate tooling lives in a separate repository cloned **inside** this repo at `transifex/`
(the path is gitignored):

```bash
git clone git@code.developers.mega.co.nz:mobile/android/transifex.git transifex
cp transifex/weblate/translate.json.example transifex/weblate/translate.json
```

Then edit `transifex/weblate/translate.json` and set `SOURCE_TOKEN` to your personal Weblate
API token: in Weblate (https://translate.developers.mega.co.nz) go to **Your profile → API
access** and copy the token. The Claude Code hook reads the token from this file; without it
the hook can only ask you to confirm instead of verifying.

Claude Code will prompt you once to approve the project hooks when they first appear or change
— that approval is what activates the push gate in your sessions.

## CI setup (one-time, Jenkins admin)

The **Weblate Strings Check** stage needs a Jenkins *Secret text* credential with ID
`WEBLATE_TOKEN` containing a Weblate API token (a service/bot account token is preferable to a
personal one). Create it under **Manage Jenkins → Credentials → Global**, matching how
`ANTHROPIC_API_KEY` is provisioned for the Code Review stage. Until the credential exists, the
stage reports "cannot verify" as a warning comment on every string-changing MR rather than
failing the pipeline.
