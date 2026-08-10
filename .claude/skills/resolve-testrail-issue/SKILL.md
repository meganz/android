---
name: resolve-testrail-issue
description: >
  Backport a fix to a release branch and resolve the TestRail test that
  reported it. Takes a Jira key, TestRail test ID, or commit SHA,
  cherry-picks the fix from develop onto the target release branch, pushes,
  then posts a "Fixed" result back to the TestRail test (reassigning to the
  original reporter with a comment). The test can be reopened later if QA
  fails it again — re-run the skill each time.
triggers:
  - /resolve-testrail-issue
  - resolve testrail issue
  - cherry pick to release
  - backport to release
  - cherry-pick fix and update testrail
---

# Resolve TestRail Issue

Use this skill when a TestRail test has been reported failed, the fix has
landed on `develop`, and the fix needs to be backported to an active
release branch with the TestRail test updated to "Fixed" and reassigned to
the original reporter. A test may go through this cycle multiple times
(fail → fixed → fail again → fixed again) before final QA sign-off — run
the skill each time.

## Inputs

**Required from the user (both, every run):**

1. A commit identifier — TestRail test ID is the typical form (e.g. `T21372415`)
2. The target release branch (e.g. `release/v16.7`)

If either is missing, stop and ask. Do NOT guess the release branch from
`git branch -r` — release branch choice belongs to the user.

| Input | Forms accepted | If missing |
|---|---|---|
| **Commit identifier** | Jira key (`AND-23617`), TestRail test ID (`T21370950` or `21370950`), or commit SHA (`8cf459def4`). Combined forms like `AND-23617/T21370950` are common. | Required — must come from the user. |
| **Target release branch** | `release/v16.7`, `release/v16.8`, etc. | Required — must come from the user. Show `git branch -r \| grep release/v` as a reference list when asking, but never pick one yourself. |
| **Comment text** | Free text. | **Default:** `I have fixed the issue, please check it again next build` — use as-is unless the user supplies something else. Reject placeholders like "bla bla"; TestRail comments are public and cannot be deleted without admin rights. |

## Step-by-step

### 0. Preflight: TestRail MCP setup

Before doing any git work, check that the TestRail MCP is configured.
Without it the skill can't post the "Fixed" result and you'd cherry-pick
to a release branch only to dead-end at step 8.

**`.mcp.json` is the single source of truth** for TestRail credentials in
this skill — both the MCP server and the curl fallback (step 8) read from
it.

Check the testrail block:

```bash
grep -A6 '"testrail"' .mcp.json 2>/dev/null
```

If the `testrail` block is missing, or any of `TESTRAIL_URL`,
`TESTRAIL_USERNAME`, `TESTRAIL_API_KEY` is empty, STOP and walk the user
through setup:

1. **Get the API key from TestRail UI:**
   - Open `https://testrail.systems.mega.nz/index.php?/mysettings`
     (My Settings page; the **API Keys** tab is in the left sidebar)
   - Click **Add Key**, give it a name (e.g. `claude-code`), copy the key
     immediately (it's only shown once)
2. **Add to `.mcp.json`** in this project:
   ```json
   "testrail": {
       "command": "npx",
       "args": ["-y", "@bun913/mcp-testrail@latest"],
       "env": {
           "TESTRAIL_URL": "https://testrail.systems.mega.nz",
           "TESTRAIL_USERNAME": "<your TestRail email>",
           "TESTRAIL_API_KEY": "<paste key here>"
       }
   }
   ```
3. **Restart Claude Code** so the MCP server loads. The `mcp__testrail__*`
   tools only register on session start — editing `.mcp.json` mid-session
   does NOT pick up the new server.
4. After restart, re-run `/resolve-testrail-issue`.

If `.mcp.json` already has the testrail block but `mcp__testrail__*` tools
aren't loaded in this session, the user added the entry mid-session — same
fix: restart Claude Code.

### 1. Save working state

Before touching branches:

```bash
git status                        # check current branch + uncommitted work
git stash push -u -m "WIP: <branch-name>"   # only if there's uncommitted work
```

Capture the current branch name so you can restore it at the end.

**Submodule note:** `sdk/src/main/jni/mega/sdk` and `…/megachat/sdk` often
show as "modified" after branch switches. That's working-tree state, not
something to commit. Leave them alone.

### 2. Fetch the branches you need

```bash
git fetch origin develop
git fetch origin <target-release-branch>
```

If `git fetch --all` fails with "not our ref" errors on submodules, that's
expected — fetch the specific top-level branches instead.

### 3. Locate the commit and verify the release branch

TestRail test IDs have the form `T<number>` (e.g. `T21372415`); the `T`
prefix is optional when the user provides input. Strip the leading `T` to
form the bare numeric ID before grepping.

#### 3a. Find the fix commit on develop

Substitute the bare number into the grep pattern (this example uses
`21372415`):

```bash
git log --oneline origin/develop -200 | grep -iE "T?21372415"
```

- **SHA given:** verify it exists on develop: `git log --oneline origin/develop -200 | grep <sha>`
- **Jira key:** same pattern with the key, e.g. `grep -iE "AND-23617"`
- If there are multiple matches, show them and ask which one is the fix.
  If there's exactly one, proceed without asking.
- Show the commit (subject + author + date) before moving on.

#### 3b. Verify the user-supplied release branch

The target branch is a required input — never pick one yourself. If the
user didn't name one, stop and ask. To help them, list available branches:

```bash
git branch -r | grep -E 'origin/release/v[0-9]' | sort -V
```

Verify the user-supplied branch exists on `origin/` before cherry-picking.
If it doesn't, surface the list above and ask again — don't fall back to a
similar-looking name.

### 4. Cherry-pick onto the release branch

```bash
git checkout <target-release-branch>
git pull origin <target-release-branch>
git cherry-pick <sha>
```

**If conflicts:** stop. Run `git status`, surface the conflicted files, and
let the user resolve. Do NOT auto-resolve or use `--strategy theirs/ours`.

**After cherry-pick succeeds:** show `git log --oneline -3` and `git status`.
The submodule "modified" lines are working-tree noise (see step 1) and will
not be pushed.

### 5. Confirm before pushing — MANDATORY GATE

**Always stop here and ask.** Pushing to a release branch is visible to
the team and affects shared state. Even if the user originally said "do
everything", this gate stays — confirmation must be re-obtained for THIS
specific cherry-pick SHA on THIS specific release branch.

Show the user the cherry-picked SHA, the target branch, and ask for an
explicit yes/no before running:

```bash
git push origin <target-release-branch>
```

If the user says no, stop the workflow — do NOT continue to the TestRail
update. Leave the commit local so they can decide later.

### 6. Restore working state

```bash
git checkout <original-branch>
git stash pop                     # only if you stashed in step 1
git status                        # confirm clean restore
```

### 7. Confirm before TestRail update

Skip TestRail entirely if no T##### was given. Otherwise, ASK the user
to confirm:
- The comment text — propose the default `I have fixed the issue, please check it again next build` as the recommended option; only swap in custom wording if the user explicitly provides it. Reject placeholders like "bla bla".
- The intent to reassign to the original reporter

### 8. Update TestRail

**Reference values for MEGA TestRail:**

| Item | Value / Source |
|---|---|
| Base URL | `.mcp.json` → `testrail.env.TESTRAIL_URL` (currently `https://testrail.systems.mega.nz`) |
| Username | `.mcp.json` → `testrail.env.TESTRAIL_USERNAME` |
| API key | `.mcp.json` → `testrail.env.TESTRAIL_API_KEY` |
| **Status: Fixed** | **`status_id = 8`** (custom status, NOT a TestRail default) |
| Other statuses | 1=Passed, 2=Blocked, 3=Untested, 4=Retest, 5=Failed |

**Two ways to call the TestRail API:**

1. **Preferred: MCP tools** (`mcp__testrail__*`) — available if `.mcp.json`
   has a `testrail` server entry and the session was started after that was
   configured. Use `mcp__testrail__getTest`, then `mcp__testrail__addResultForCase`.

2. **Fallback: curl** — use when MCP tools aren't loaded yet (e.g., key was
   just added/regenerated mid-session and Claude Code hasn't been
   restarted). Read URL, username, and key fresh from `.mcp.json` each time.

**Workflow:**

```
GET  /api/v2/get_test/{test_id}
       → returns case_id, run_id, current assignedto_id, status_id
GET  /api/v2/get_results/{test_id}
       → returns history; find the Failed result's created_by → that's the reporter
POST /api/v2/add_result_for_case/{run_id}/{case_id}
       body: {
         "status_id": 8,
         "comment": "<user's comment>",
         "assignedto_id": <reporter user_id>
       }
```

curl example (all credentials sourced from `.mcp.json` — no hardcoded
email, no second credential file):
```bash
# jq is preferred; falls back to python3 if jq isn't installed
read_env() {
    jq -r ".mcpServers.testrail.env.$1" .mcp.json 2>/dev/null \
        || python3 -c "import json,sys; print(json.load(open('.mcp.json'))['mcpServers']['testrail']['env']['$1'])"
}
URL=$(read_env TESTRAIL_URL)
USERNAME=$(read_env TESTRAIL_USERNAME)
KEY=$(read_env TESTRAIL_API_KEY)

curl -s -u "$USERNAME:$KEY" \
  -H "Content-Type: application/json" \
  -X POST \
  -d '{"status_id":8,"comment":"<comment>","assignedto_id":<reporter_id>}' \
  "$URL/index.php?/api/v2/add_result_for_case/<run_id>/<case_id>"
```

### 9. Report

Tell the user, in one or two sentences:
- The cherry-pick commit SHA on the release branch
- That the push succeeded
- The TestRail result ID and a link to the test:
  `https://testrail.systems.mega.nz/index.php?/tests/view/<test_id>`

## Guardrails

- **Never push without explicit confirmation for THIS run.** Release
  branches are shared. A general "do everything" at skill start does NOT
  count — re-ask after the cherry-pick succeeds, with the SHA and branch
  visible in the question.
- **Never post a TestRail comment with placeholder text.** Reject "bla bla",
  "TBD", or empty strings — ask for the real wording.
- **Don't run `clean`, `reset --hard`, or `checkout .`** to clear submodule
  noise. It's harmless working-tree state.
- **Don't skip stash/restore.** If you switch branches with uncommitted
  work, the user loses context.
- If the TestRail API returns 401, the key in `.mcp.json` is stale or
  wrong. Ask the user to regenerate it via
  `https://testrail.systems.mega.nz/index.php?/mysettings` → **API Keys**
  rather than guessing usernames.

## Rollback

If the wrong commit was pushed to the release branch or the wrong status
was posted to TestRail:

1. **Cherry-picked the wrong commit:** revert it on the release branch and
   push the revert — never force-push.
   ```bash
   git checkout <release-branch>
   git pull
   git revert <bad-sha>
   git push origin <release-branch>
   ```
2. **Posted the wrong TestRail result:** TestRail results cannot be deleted
   without admin rights. Add a new result on the same test with status
   `4 = Retest` and a comment explaining the correction (e.g. "previous
   Fixed result was posted in error, please disregard"). The reporter
   reassignment from the bad result stays; reassign again as needed in the
   new result.

## Reference: status IDs

Discovered via `GET /api/v2/get_statuses` on MEGA's TestRail:

| ID | Name | Source |
|---|---|---|
| 1 | Passed | system |
| 2 | Blocked | system |
| 3 | Untested | system |
| 4 | Retest | system |
| 5 | Failed | system |
| 6 | Parked | custom |
| 7 | Skipped | custom |
| **8** | **Fixed** | **custom** |
| 9 | FutureDev | custom |
| 10 | Feedback | custom |
| 11 | In Progress | custom |
