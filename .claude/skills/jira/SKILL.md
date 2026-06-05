---
name: jira
description: >
  Manage MEGA Jira tickets (Server / Data Center) from the command line.
  Supports starting a ticket (transition to In Progress), submitting for
  review (transition to Tech QA + auto-comment with a QA-friendly summary
  built from git history), posting arbitrary comments, and populating the
  Test Instruction custom field from a generated wiki-markup body.
triggers:
  - /jira
  - jira start
  - jira submit
  - jira comment
  - jira status
  - jira test-instructions
---

# Jira

Drive MEGA Jira tickets (project `AND`, Server / Data Center) from the
terminal. All deterministic logic — HTTP, token loading, transition matching,
field discovery, branch parsing, diff classification — lives in
`tools/jira/jira_cli.py`. **This SKILL.md only orchestrates the
CLI and handles LLM-judgment work** (text generation, prompting, decisions).

## Usage

```
/jira create [--type Bug] [--from <file>]    # Create a ticket (interactive / from a report)
/jira start [TICKET]                         # Transition to In Progress
/jira submit [TICKET] [--no-comment] [--no-transition]   # Tech QA + comment
/jira submit [TICKET] --mr-url <url>         # Embed MR URL in the comment
/jira submit [TICKET] --target <status>      # Override the transition target (default: Tech QA / Code Review / ...)
/jira comment <text> [TICKET]                # Post a free-form comment
/jira status [TICKET]                        # Show status + transitions (debug)
/jira test-instructions [TICKET] [--force] [--dry-run]   # Set Test Instruction
```

`TICKET` is optional — extracted from the current git branch when omitted.

## Configuration

Defaults baked into the CLI (`jira_cli.py`):

| Setting        | Value                                                |
|----------------|------------------------------------------------------|
| Jira Base URL  | `https://jira.developers.mega.co.nz` (override via `$JIRA_BASE_URL`) |
| Jira flavor    | **Server / Data Center**, REST `/rest/api/2/`, Bearer PAT |
| Project key    | `AND`                                                |
| Token source   | env `JIRA_TOKEN` (or legacy `JIRA_ACCESS_TOKEN`); fallback to `export ...=` in `~/.zshrc` |
| Token handling | written to a 0600 temp file consumed by `curl -H @file` — never on argv, never echoed |
| Network        | `curl` honors `ALL_PROXY` (direct / VPN / SOCKS5 all work) |

To point at a different Jira (sandbox, staging, clone, future Cloud
migration), `export JIRA_BASE_URL=https://...` before invoking the CLI. No
source edit required.

## CLI reference

All subcommands live in `tools/jira/jira_cli.py` (Python 3, stdlib only — no
`pip install` needed). Invoke through the thin bash wrapper:

```
tools/jira/jira <subcommand> ...
```

**First-time setup** (per clone):

```bash
chmod +x tools/jira/jira
```

If `chmod` is not available (e.g. inside a sandboxed harness), fall back to
`bash tools/jira/jira <subcommand>` — it works the same.

**Python requirement**: `python3` must be on `PATH`. macOS 12.3+ (March 2022)
ships it by default; otherwise run `xcode-select --install`. The wrapper
checks for `python3` and prints a helpful error if missing.

| Subcommand                                     | Effect                                                          |
|------------------------------------------------|-----------------------------------------------------------------|
| `create --summary <s> [--type Bug] [--project AND] [--component <c>]... [--label <l>]... [--priority <p>] [--select cf=val]... [--field cf=<json>]... [--dry-run]` (description on stdin) | Create an issue. Prints `{key, id, browse_url}` (HTTP 201). `--select cf=val` wraps a single-select field as `{"value": val}`; `--field cf=<json>` sets a raw JSON value. `--dry-run` prints the payload and makes NO network call. Refuses empty stdin unless `--allow-empty-description`. |
| `search "<jql>" [--fields ...] [--max N]`      | JQL search → `{total, issues:[{key, summary, status, browse_url}]}`. Use for dedup before `create`. |
| `components [--project AND]`                    | JSON `[{id, name}]` of the project's components (valid `--component` names) |
| `create-meta [--project AND] [--type Bug]`     | JSON of create-screen fields `[{id, name, required, allowedValues}]`, required first. Run before `create` to confirm what the project mandates instead of hardcoding field ids. |
| `status <KEY>`                                 | JSON of key, summary, status, assignee, test_instruction_empty  |
| `transitions <KEY>`                            | JSON array of `{id, name, to}`                                  |
| `pick-transition <KEY> "<csv-target-statuses>"`| JSON of best-matching transition (by `to.name`) or exit 4       |
| `transition <KEY> <ID>`                        | Execute transition (expects HTTP 204)                           |
| `comment <KEY>` (body on stdin)                | JSON `{id, url}` of the new comment                             |
| `field-id "<csv-field-name-candidates>"`       | JSON `{id, name, type}` of the matched custom field             |
| `read-field <KEY> <FIELD_ID>`                  | Print raw value to stdout (empty if null)                       |
| `update-field <KEY> <FIELD_ID> [--allow-empty]` (value on stdin) | PUT the field (HTTP 204). Refuses empty stdin unless `--allow-empty` is passed. |
| `branch-ticket`                                | Print first `AND-\d+` from current branch (or exit 1)           |
| `branch-name <args...>`                        | Build `<user>/<TICKET>-<slug>` from args. AND-NNNN is stripped from every arg before slugifying, so `AND-1234-fix-login` and `AND-1234 fix login` both produce `<user>/AND-1234-fix-login`. If the first arg contains `/`, it's used as a full branch name verbatim. |
| `classify-diff [--base develop]`               | JSON `{needs:[], skip:[], total}` of changed paths              |
| `selftest [-v]`                                | Run stdlib unittest assertions on pure-Python helpers; no network. |

Exit codes:

| Code | Meaning                                                              |
|------|----------------------------------------------------------------------|
| `0`  | OK                                                                   |
| `1`  | **Caller-side error — read stderr to disambiguate.** Includes: HTTP 404 (issue not found); `branch-ticket` finding no AND-NNNN in branch; `classify-diff` base ref missing or git failure; `update-field` empty stdin without `--allow-empty`; `*-validate` invalid issue key / field id / transition id; **any other 4xx the caller can fix** (e.g. HTTP 400 field-too-long, 409 bad transition for current status, 422 validation). |
| `2`  | Auth — `JIRA_TOKEN` / `JIRA_ACCESS_TOKEN` missing, rejected, or contains newline (header-injection guard). |
| `3`  | Network / unexpected HTTP — curl failed, VPN/SOCKS5 down, or Jira returned a status the CLI doesn't expect (e.g. HTTP 500). |
| `4`  | No matching transition target — stderr lists the available `to.name` values. |
| `5`  | No matching custom field — stderr lists the candidates tried.        |

## Verified facts (MEGA Jira, AND project)

- Test Instruction field = `customfield_10400` (single, type `string`/textarea)
- AND **Bug create** required fields, confirmed via `create-meta` (2026-06):
  `summary`, `issuetype` (`{"name": "Bug"}`), `project` (`{"key": "AND"}`),
  `components` (**required** — at least one valid project component), and
  `customfield_10501` "Expense Product" (**required**, e.g.
  `{"value": "Cloud/Default"}`). `customfield_10500` "Expense Type"
  (`OPEX`/`CAPEX`) is optional. The legacy single-shot `GET
  /issue/createmeta?projectKeys=...` was removed in Jira 9.0+ (404s as "Issue
  Does Not Exist"); the CLI's `create-meta` uses the per-issuetype endpoints
  `/issue/createmeta/{project}/issuetypes[/{id}]`. Still **confirm at runtime**
  — screen config drifts; never hardcode blindly.
- Issue transitions for AND tickets (workflow-dependent; **never hardcode the
  ids — always discover at runtime**):

  | Target status         | Transition name observed       |
  |-----------------------|--------------------------------|
  | In Progress           | "Start Progress"               |
  | Tech QA               | "Send to Tech QA"              |
  | Resolved              | "Resolve Issue"                |
  | Closed                | "Close Issue"                  |
  | Ready for Estimation  | "Definition of ready satisfied" |

  This is why we match by `transition.to.name`, not by `transition.name`.

  Moreover, **`transition.name` and `transition.id` are not stable for a
  given target status across source statuses**. Concrete example:

  | Source status | → In Progress transition |
  |---------------|--------------------------|
  | Open          | `name="Start Progress"` (id=801) |
  | Tech QA       | `name="Return to development"` (id=771) |

  Same `to.name` ("In Progress"), different transition. Always discover
  the transition by current `to.name` at the moment of execution; never
  cache an id.

  Observed available outgoing target statuses, by source (workflow snapshot
  — informational, not authoritative; treat as a hint, always discover):

  | Source        | Outgoing target statuses |
  |---------------|--------------------------|
  | Open          | In Progress, Tech QA, Resolved, Closed, Ready for Estimation |
  | In Progress   | Tech QA, QA, Feedback, Resolved, Closed, Open                |
  | Tech QA       | QA, Feedback, In Progress, Resolved, Closed                  |

  Note: `QA` and `Tech QA` are distinct statuses.

- **Test Instruction field length limit**: `customfield_10400` accepts up
  to **32,767 characters**. Beyond that, Jira returns HTTP 400 with body
  `"The entered text is too long. It exceeds the allowed limit of 32,767
  characters."` Being an HTTP 400, the CLI surfaces this as **exit 1**
  (caller error — the body must be shortened), not exit 3. The
  auto-generated body template (≤ 25 lines) stays well under this limit;
  only a `--force` with user-supplied content is realistically at risk.

- HTTP status codes:

  | Endpoint                              | Method | Success |
  |---------------------------------------|--------|---------|
  | `/issue` (create)                     | POST   | **201** |
  | `/issue/{key}` (read)                 | GET    | 200     |
  | `/issue/{key}/transitions` (list)     | GET    | 200     |
  | `/issue/{key}/transitions` (execute)  | POST   | **204** |
  | `/issue/{key}/comment`                | POST   | **201** |
  | `/issue/{key}` (field update)         | PUT    | **204** |
  | `/issue/createmeta`                   | GET    | 200     |
  | `/search`                             | GET    | 200     |
  | `/project/{key}/components`           | GET    | 200     |
  | `/field`                              | GET    | 200     |

  The CLI does this check for you (`expect_status`) — surface its non-zero
  exit codes instead of re-implementing.

## Subcommand flows

### `/jira create [--type Bug] [--from <file>]`

Creates a new ticket in the `AND` project. The CLI handles the HTTP POST,
payload assembly, and validation; **you** handle the LLM-judgment work:
writing the summary + description, choosing the component/priority/labels,
and confirming with the user before anything is created.

1. **Gather the source material.** Either:
   - the user describes the bug/task inline, or
   - `--from <file>` points at a report (e.g. a security-scan markdown). Read
     it and treat each finding as one candidate ticket — see *Creating tickets
     from a security report* below.

2. **Confirm the create screen's requirements** (do this once per session, not
   per ticket):

   ```bash
   tools/jira/jira create-meta --project AND --type Bug
   ```

   Note which fields are `required: true` and the `allowedValues` for any
   select fields. Confirmed on MEGA's AND Bug screen (2026-06): the required
   create fields are `summary`, `issuetype`, `project`, **`components`**, and
   **`customfield_10501`** ("Expense Product"). The relevant selects:

   | Field               | Flag                              | Required? | Value used / allowed |
   |---------------------|-----------------------------------|-----------|----------------------|
   | `customfield_10501` | `--select customfield_10501=...`  | **yes**   | `Cloud/Default` (also Chat/Meetings, Object Storage, VPN, Password Manager, Transfer.it) |
   | `customfield_10500` | `--select customfield_10500=...`  | no        | `OPEX` (or `CAPEX`)  |
   | `components`        | `--component "<name>"`            | **yes**   | a valid project component (see step 3) |
   | `priority`          | `--priority <name>`               | no        | Highest / High / Medium / Low / Lowest |

   If `create-meta` reveals an additional required field, add it via `--select`
   (single-select) or `--field cf=<json>` (anything else) — don't proceed and
   let the POST 400. If `create-meta` fails with exit 3, the network is down;
   halt with the network remediation (see Error handling).

3. **Pick a component** (optional but recommended). List valid names and map
   the affected code area to one:

   ```bash
   tools/jira/jira components --project AND
   ```

   Pass the matched name with `--component "<name>"`. Never invent a component
   name — an unknown one 400s.

4. **Search for an existing ticket** (dedup — skip only if the user insists).
   Build a JQL that targets a distinctive phrase from the summary. **Quote the
   project key** — `AND` is also a JQL keyword, so `project = AND` is a 400;
   use `project = "AND"`:

   ```bash
   tools/jira/jira search 'project = "AND" AND statusCategory != Done AND summary ~ "ANDROID_ID AES key"'
   ```

   If `total > 0`, show the user the matching `key`/`summary`/`status` and ask
   whether to still create a new one. Default to **not** duplicating.

5. **Compose summary + description** (LLM-judgment). Summary: one concise line,
   prefixed to aid triage (e.g. `[Security] ...`). Description: Jira wiki
   markup. A general template:

   ```
   h3. Summary
   <1-2 sentences>

   h3. Steps to Reproduce / Context
   # <step or context bullet>

   h3. Expected vs Actual
   * Expected: <...>
   * Actual: <...>

   h3. References
   * <file:line, link, or scan id>
   ```

6. **Dry-run and confirm.** Always show the user the payload first:

   ```bash
   printf '%s' "$BODY" | tools/jira/jira create \
     --summary "<summary>" --type Bug \
     --component "<component>" --priority <High|Medium|Low> \
     --label <label> \
     --select customfield_10500=OPEX --select customfield_10501=Cloud/Default \
     --dry-run
   ```

   Use `AskUserQuestion`: `Create this AND <type>?` → `Create` / `Edit then
   create` / `Skip`. Creating a Jira ticket is an outward-facing action — never
   create without explicit confirmation.

7. **Create** (drop `--dry-run`) only after the user approves. The CLI prints
   `{key, id, browse_url}`. Report the `browse_url` to the user.

#### Creating tickets from a security report

When `--from` points at a scan report (e.g.
`reports/mega-android.md`) containing many findings:

- Treat each finding as one candidate Bug. Map its fields:
  - **Summary** ← finding title, prefixed `[Security] `.
  - **Priority** ← severity (`HIGH`→`High`, `MEDIUM`→`Medium`, `LOW`→`Low`).
  - **Labels** ← `security`, `agentic-security-scan` (provenance — marks the
    ticket as coming from the automated scan, not a human report), plus a CWE
    label if present (e.g. `CWE-321`).
  - **Component** ← `Android`. (MEGA files Android security-scan tickets under
    the broad `Android` component rather than per-feature components, so the
    whole batch is easy to find and triage together.)
  - **Description** ← include the file:line, CWE, attacker path, missing
    guard, and impact from the report, in the wiki-markup template above, with
    a footer noting it is UNVERIFIED automated-scan output to be confirmed.
- **De-duplicate first**: many findings in a report are the *same* root cause
  (e.g. the report lists the ANDROID_ID AES key twice, and the WebView
  debugging issue twice). Collapse duplicates into one ticket before creating.
- The report header may warn findings are **unverified proposer output**. Tell
  the user this and confirm scope before creating — offer to create only the
  HIGH-confidence findings, a chosen subset, or all of them.
- **Batch confirmation, not silent loops**: present the full list of proposed
  tickets (summary + severity + component) in one table and get a single
  go-ahead, then create them one at a time, reporting each `browse_url`. Do a
  `--dry-run` of at least the first before the batch. If `search` finds an
  existing ticket for a finding, skip it and say so.

### `/jira start [TICKET]`

1. **Resolve ticket**: if `TICKET` arg passed, use it; else
   `tools/jira/jira branch-ticket`.

2. **Idempotence check**: run `... status <KEY>`. If `.status == "In Progress"`,
   report `Already in In Progress — nothing to do.` and stop.

3. **Pick a transition** to In Progress:

   ```bash
   tools/jira/jira pick-transition <KEY> "In Progress"
   ```

   If exit code 4, surface the CLI's stderr (which lists available targets)
   and halt — never guess.

4. **Execute** the transition with the returned id:

   ```bash
   tools/jira/jira transition <KEY> <ID>
   ```

5. **Verify** with `... status <KEY>` and report new status to the user.

### `/jira submit [TICKET] [--mr-url <url>] [--no-comment] [--no-transition]`

1. **Resolve ticket** + check there's work to submit.

   First confirm the base ref exists — otherwise the count below silently
   returns 0 and masks a missing branch as an empty diff (same trap
   `classify-diff` guards against):

   ```bash
   git rev-parse --verify --quiet develop >/dev/null \
     || { echo "local develop not found — run 'git fetch origin develop:develop' and retry."; }
   ```

   If the ref is missing, halt with that remediation. Otherwise count:

   ```bash
   test "$(git log develop..HEAD --pretty=format:%h | wc -l)" -gt 0
   ```

   If zero, halt: `No commits on this branch ahead of develop — nothing to
   submit. Commit your work first.`

2. **Build the comment body** (skip if `--no-comment`). This is LLM-judgment
   work — read `git log develop..HEAD --pretty='format:%h %s'` and
   `git diff --stat develop...HEAD`, then synthesize Jira wiki markup using
   this template. Be concrete, QA-actionable, and present-tense:

   ```
   h3. Submitted for Review
   <MR link if --mr-url given, otherwise omit this line>

   h3. Summary
   <1-2 sentences synthesised from commit messages>

   h3. Changes
   * <bullet per logically-distinct change>

   h3. Affected Modules
   * <top-level Gradle module dirs touched, max 8>

   h3. Testing Notes
   * <how QA should validate — happy path + edge cases>
   ```

   Rules: skip test/docs/build noise from the changes list; for data/domain-
   only changes, describe how to exercise via existing UI; if a feature flag
   is introduced, step 1 must be enabling it.

3. **Post the comment** by piping the body to the CLI:

   ```bash
   printf '%s' "$BODY" | tools/jira/jira comment <KEY>
   ```

   The CLI prints `{"id": "...", "url": "..."}`. Capture the URL for the
   final report.

4. **Pick transition** (skip if `--no-transition`):

   ```bash
   tools/jira/jira pick-transition <KEY> \
     "Tech QA,Code Review,In Review,Ready for Review"
   ```

   If `--target <name>` was passed, pass that single value instead.

5. **Execute** the transition. Skip if already in the target status (read
   current status via `... status <KEY>` before transitioning — same
   idempotence logic as `start`).

6. **Report** to the user — Jira link + comment URL + new status.

### `/jira comment <text> [TICKET]`

1. Resolve ticket as above.
2. Post via the CLI; the text comes on stdin:

   ```bash
   printf '%s' "$TEXT" | tools/jira/jira comment <KEY>
   ```

3. Report the comment URL from the CLI's JSON output.

### `/jira status [TICKET]`

Just call `... status <KEY>` (and optionally `... transitions <KEY>`) and
render the result as a markdown table for the user.

### `/jira test-instructions [TICKET] [--force] [--dry-run]`

The CLI handles steps 1, 2, and 4 (field discovery, read, classify). You
handle steps 5 and 6 (generate body, prompt user).

1. **Resolve ticket** + **discover field id**:

   ```bash
   KEY=$(tools/jira/jira branch-ticket)
   FIELD=$(tools/jira/jira field-id \
     "Test Instructions,Test Instruction,QA Steps,Testing Instructions" \
     | python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])')
   ```

   If `field-id` exits 5, the Jira instance doesn't have such a field — halt.

2. **Read current value**:

   ```bash
   CURRENT=$(tools/jira/jira read-field <KEY> "$FIELD")
   ```

   Treat `null`, empty, or a placeholder like `_TBD_` / `N/A` (case-insensitive)
   as "empty".

3. **Gate on populate vs overwrite**:
   - Populated and `--force` NOT passed → report `Already populated, skipping.`
     Halt.
   - Populated and `--force` passed → continue (warn the user existing value
     will be overwritten).
   - Empty → continue.

4. **Classify the diff** to decide whether the change *needs* Test Instructions:

   ```bash
   tools/jira/jira classify-diff
   ```

   Capture both the exit code AND stdout. Distinguish three outcomes:

   - **rc=0, `needs` non-empty** → continue to Step 5 (generate body).
   - **rc=0, `needs` empty** → the diff is test/docs/build/tooling-only.
     Halt with `Change is test/docs/build/tooling-only — no Test Instructions
     needed.`
   - **rc=1** → the CLI could not run the diff. Read stderr for the cause:
     - `base ref ... not found` → tell the user to run the suggested
       `git fetch origin ...` command, then halt the Test Instruction step.
     - Anything else → surface the stderr verbatim and halt.

   Never treat rc=1 the same as rc=0-with-empty-needs — a missing base ref
   masks real productive code.

5. **Generate body** (LLM-judgment): read the actual diff with
   `git diff develop...HEAD -- <files-in-needs>` and synthesize wiki markup:

   ```
   h3. Pre-conditions
   * <env / account / feature flag / data prerequisites — be specific>

   h3. Steps to Verify
   # <user-facing action 1>
   # <user-facing action 2>

   h3. Expected Result
   * <observable outcome>

   h3. Edge Cases / Regression Checks
   * <related flow that could break>
   ```

   Rules: reflect what the diff actually changes (do not invent features);
   prefer concrete UI labels / screen names from the diff; if a new feature
   flag appears, step 1 is enabling it; total ≤ 25 lines; omit sections that
   don't apply rather than padding with `N/A`.

6. **Prompt** before writing (skip if `--dry-run` — then print and exit):

   Use `AskUserQuestion`:
   - Q: `Update Test Instruction on <KEY>?` (mention empty vs overwrite)
   - Options: `Apply` / `Edit then apply` / `Skip`

7. **Write** (only on `Apply`):

   ```bash
   printf '%s' "$BODY" | tools/jira/jira update-field <KEY> "$FIELD"
   ```

   CLI prints nothing on success; non-zero exit means failure.

   The CLI **refuses an empty stdin by default** (rc=1) to prevent silent
   field wipes from an unset/unbound shell variable. If the user explicitly
   confirms a clear (e.g. resetting the sacrificial test ticket), pass
   `--allow-empty`:

   ```bash
   : | tools/jira/jira update-field <KEY> "$FIELD" --allow-empty
   ```

8. **Report** with the outcome (`updated (was empty)` / `updated (overwrote
   existing)` / `skipped (already populated)` / `skipped (no productive code
   change)` / `skipped (user declined)`).

## Error handling

- **CLI exit 2** (auth) → halt with the CLI's own stderr message, which lists
  both `JIRA_TOKEN` and `JIRA_ACCESS_TOKEN` as valid env var names.
- **CLI exit 3** (network) → halt with: `Jira unreachable. VPN / SOCKS5 /
  corporate proxy may be down — check whichever transport you normally use.`
- **CLI exit 1** → context-dependent; read stderr:
  - HTTP 404 / "Issue Does Not Exist" → confirm the ticket key is correct.
  - `branch-ticket`: `No AND-NNNN ticket key in branch ...` → pass the key
    explicitly.
  - `classify-diff`: `base ref ... not found` → run the suggested
    `git fetch origin ...` and retry. **Do not** treat this the same as an
    empty `needs` array.
  - `update-field` without `--allow-empty` → stdin was empty; pass
    `--allow-empty` only if clearing is truly intended.
  - `*-name validation`: `invalid issue key/field id/transition id` → caller
    passed a malformed argument; do not retry without sanitizing.
- **CLI exit 4** (no matching transition) → CLI already lists available
  targets in stderr; surface that to the user and stop. **Never guess.**
- **CLI exit 5** (no matching field) → halt; do not invent a field name.

## Notes

- The skill never invokes destructive Jira operations (transitions that
  cannot be undone in this workflow without admin) silently. `/jira start`
  and `/jira submit` are user-initiated.
- For multi-ticket branches (rare — e.g. stacked work), the first
  `AND-\d+` wins. Pass `TICKET` explicitly to override.
- Designate a sacrificial ticket (one safe to mutate) for end-to-end testing
  of this skill. Before re-running against it, reset it to `Open` in the
  Jira UI and clear its Test Instruction field first.
