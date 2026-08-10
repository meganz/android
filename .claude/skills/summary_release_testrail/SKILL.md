---
name: summary_release_testrail
description: >
  Summarise a release's Failed / Feedback TestRail cases and post the standard
  "Android team" notice to Slack. Takes a version (e.g. v16.8), finds the matching
  release run in TestRail project 5, collects every test marked Failed or Feedback
  (excluding automated-test failures), groups them by assignee with Slack @-mentions
  and TestRail links, then sends the message to a given #channel or thread reply.
  If no Failed/Feedback cases are found, it sends nothing.
triggers:
  - /summary_release_testrail
  - summary release testrail
  - summarise release testrail
  - release testrail slack notice
---

# Summarise Release TestRail → Slack notice

Generates and posts the standard release-quality notice, e.g.:

```
/summary_release_testrail v16.8                       (posts to #android-dev-team, the default)
/summary_release_testrail v16.8 → #some-other-channel  (override the channel)
/summary_release_testrail v16.8 → reply to <thread permalink>
```

The message format is fixed (see **Step 6**). The skill does everything:
resolve the run from the version name, pull Failed + Feedback tests, resolve
assignees to Slack mentions, build the message, and post it after a
confirmation gate.

## Inputs

| Input | Forms accepted                                                                                          | If missing |
|---|---------------------------------------------------------------------------------------------------------|---|
| **Version** | `v16.8`, `16.8` (the leading `v` is optional).                                                          | Required — must come from the user. |
| **Destination** | A channel (`#android-dev-team` or a channel link), OR a thread to reply to (a Slack message permalink). | **Defaults to `#android-dev-team`.** Use it unless the user names a different channel or a thread. |

Strip a leading `v` from the version before matching run names.

## Preflight: setup (do this first, every run)

This skill needs **two** integrations. If either is missing, stop and walk the
user through setup, then resume.

### 1. TestRail (same setup as `/resolve-testrail-issue`)

`.mcp.json` is the single source of truth for TestRail credentials. Check it:

```bash
grep -A6 '"testrail"' .mcp.json
```

If the `testrail` block is missing, or any of `TESTRAIL_URL`,
`TESTRAIL_USERNAME`, `TESTRAIL_API_KEY` is empty, STOP and set it up:

1. Get an API key from `https://testrail.systems.mega.nz/index.php?/mysettings`
   → **API Keys** tab → **Add Key** (copy it immediately; shown once).
2. Add to `.mcp.json` in this project:
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
3. This skill reads those values via `curl` (the fallback path documented in
   `/resolve-testrail-issue`), so a Claude Code restart is **not** required for
   the curl calls below. A restart is only needed if you want the
   `mcp__testrail__*` tools loaded.

If the TestRail API returns `401 / Authentication failed`, the key is stale —
regenerate it via the My Settings → API Keys page rather than guessing.

> zsh gotcha: never assign the username to a shell variable named `USERNAME`
> (it's a special readonly parameter in zsh and silently resolves to the OS
> login name). Use `TR_USER` or similar, as the snippets below do.

### 2. Slack

Posting uses the **Slack connector** (`mcp__claude_ai_Slack_Reader__*` tools:
`slack_search_channels`, `slack_search_users`, `slack_send_message`,
`slack_read_thread`). If those tools aren't available, ask the user to connect
Slack in Claude Code (Settings → Connectors → Slack / claude.ai connector) and
then re-run.

## Reference values

| Item | Value |
|---|---|
| TestRail base URL | `.mcp.json` → `testrail.env.TESTRAIL_URL` |
| TestRail project (Android) | **`5`** (the `runs/overview/5` page) |
| Status: Failed | `status_id = 5` |
| Status: Feedback | `status_id = 10` |
| Test link | `https://testrail.systems.mega.nz/index.php?/tests/view/<test_id>` |

Credential helper used in every TestRail call below:

```bash
read_env() { jq -r ".mcpServers.testrail.env.$1" .mcp.json; }
URL=$(read_env TESTRAIL_URL); TR_USER=$(read_env TESTRAIL_USERNAME); KEY=$(read_env TESTRAIL_API_KEY)
TR() { curl -s -u "$TR_USER:$KEY" -H "Content-Type: application/json" "$URL/index.php?/api/v2/$1"; }
```

## Step-by-step

### 1. Resolve the run from the version name

Match the run by literal substring `release <version>` (lowercased) — do NOT
use a regex (`16.8` as a regex matches `16x8` anywhere, producing false
positives like `15.25-internal(...)`). Prefer the **alpha release** run; if
several match, pick the most recent `created_on` and, if still ambiguous, list
them and ask.

```bash
VER="16.8"   # leading v already stripped
TR "get_runs/5&limit=50" | jq --arg v "release $VER" '
  [ (.runs // .)[]
    | select(.name | ascii_downcase | contains($v))
    | {id, name, is_completed, created_on} ]
  | sort_by(.created_on) | reverse'
```

- 0 matches → tell the user no `release <version>` run exists and stop.
- 1 match → use it.
- >1 → prefer the one whose name contains `alpha`; if still >1, show the list
  and ask which run ID.

Capture the chosen `run_id`.

### 2. Fetch Failed + Feedback tests

```bash
TR "get_tests/$RUN_ID&status_id=5,10" | jq '
  [ .tests[] | {id, case_id, status_id, title, assignedto_id} ]'
```

If the array is **empty → STOP. Send nothing.** Report to the user that there
are no Failed/Feedback cases for that version (this is the explicit
"don't send if nothing found" rule).

### 3. Exclude automated-test failures

Automated failures aren't developer action items. Pull the run's results and
drop any test whose result comment contains `failed by automated test`
(case-insensitive, ignoring `*`):

```bash
TR "get_results_for_run/$RUN_ID&limit=250" | jq '
  [ .results[] | {test_id, comment} ]'
```

For each Failed/Feedback test, if any of its result comments matches
`failed by automated test`, exclude it. (Mirror the existing
`.testrail/generate_slack_update.py` behaviour.) If, after excluding, no tests
remain → STOP and send nothing.

### 4. Resolve assignee names

`get_user/<id>` (singular) needs admin and will 403. Use the project-scoped
plural endpoint, which works for normal users:

```bash
TR "get_users/5" | jq '[ (.users // .)[] | {id, name, email} ]'
```

Build `assignedto_id → {name, email}`. Tests with no `assignedto_id` go under
**Unassigned** (listed last).

### 5. Map each assignee to a Slack mention

For each assignee email, find the Slack member ID so the post @-mentions them:

- Check the **known-assignee cache** below first — if the TestRail user id /
  email is listed, use that Slack member id directly (saves a lookup).
- Otherwise `slack_search_users` with the email → take the matching `id`
  (e.g. `U096…`). Add new resolved people to the cache table for next time.
- Put `<@MEMBER_ID>` directly in the message (raw Slack mention syntax). It
  **renders as a real mention** — confirmed working via `slack_send_message`.
  Keep it OUTSIDE code spans, and do NOT collect mention codes into a footer or
  "cheat-sheet" line; mentions belong only on the per-assignee header lines.
- If no Slack user is found for an email, fall back to the **plain display
  name** (no mention) and note that in your summary to the user.

#### Known-assignee cache (TestRail project 5 → Slack)

Verified mappings; extend as new assignees appear. `get_users/5` gives
id→name→email; Slack ids confirmed via the v16.8 notice.

| TestRail id | Name | Email | Slack member id |
|---|---|---|---|
| 255 | Feng Lu | fl@mega.co.nz | `U096P7U0RHR` |
| 256 | Junjie He | juh@mega.co.nz | `U097F8Q9ANB` |
| 94 | Kevin Sun | ks@mega.co.nz | `U02ML8HT8E7` |
| 141 | Atiqur Rahman | atr@mega.co.nz | `U0420372CLQ` |
| 143 | Hai Luong | lh@mega.co.nz | `U03T1E2Q97V` |
| 228 | HM Tamim | ht@mega.co.nz | `U06BLKAJVT2` |

### 6. Build the message

Header is fixed. Group by assignee (assigned developers sorted by name first,
then `Unassigned`). One line per test: TestRail link + title + ` -Failed` or
` -Feedback`.

```
Hi Android team, a few TC has been marked as `Failed` or `Feedback` in latest Android **`vX.Y`** release. Please address them as a priority in your workloads and don't forget to update their status in TestRail (`Fixed` if you have cherry picked a fix to the release branch, `Retest` otherwise) once the fix has been completed.

<@SLACK_ID_OF_DEV_A>
[T<id>](https://testrail.systems.mega.nz/index.php?/tests/view/<id>)	<title> -Failed
[T<id>](https://testrail.systems.mega.nz/index.php?/tests/view/<id>)	<title> -Feedback

<@SLACK_ID_OF_DEV_B>
[T<id>](https://testrail.systems.mega.nz/index.php?/tests/view/<id>)	<title> -Failed
```

Notes:
- `slack_send_message` renders standard markdown: use `**bold**` and
  `[text](url)` links. Keep `` `Failed` ``, `` `Feedback` ``, `` `Fixed` ``,
  `` `Retest` `` as inline code, matching the original notice.
- Use the version exactly as the user typed it in the header (with the `v`),
  e.g. `v16.8`.

### 7. Confirmation gate — MANDATORY before posting

Posting pings real people in a shared channel. Always show the user the final
rendered message + the resolved destination (channel name/ID, or the thread
you'll reply to) and ask for an explicit yes before sending. A general "do
everything" at invocation does NOT waive this gate.

This gate matters because **the Slack connector can only send — it has no edit
or delete tool.** A wrong post can't be cleaned up programmatically; the user
must fix it manually in Slack. So get the message right before posting.

If any assignee fell back to a plain name (no Slack match), call that out here
so the user can decide whether to fix the mention before posting.

### 8. Post

- **To a channel:** resolve the channel ID with `slack_search_channels`, then
  `slack_send_message(channel_id, message)`.
- **To a thread reply:** parse the channel ID and the parent `message_ts` from
  the permalink (`.../archives/<channel_id>/p<ts>` → insert a dot 6 digits from
  the end of `<ts>`), then `slack_send_message(channel_id, message,
  thread_ts=<parent_ts>, reply_broadcast=true)`. **`reply_broadcast=true` is the
  default for thread replies** — it ticks Slack's "Also send to #channel" box so
  the notice is visible in the channel as well as the thread. Only drop it if the
  user explicitly asks to keep the reply inside the thread.

Return the resulting message link to the user.

## Guardrails

- **No Failed/Feedback (after excluding automated) → send nothing.** Just
  report the empty result.
- **Never post without explicit confirmation for this run** (Step 7).
- **Default destination is `#android-dev-team`.** Post there unless the user
  names a different channel or a thread to reply to.
- **Don't use a regex to match the version** in run names — literal
  `release <version>` substring only.
- **Don't fabricate Slack mentions.** If an email has no Slack match, use the
  plain name and flag it.
- Treat TestRail comments as public — never echo credentials or keys into Slack.

## Output

A short summary to the user: which run was used (name + ID), how many
Failed/Feedback cases by assignee, any plain-name fallbacks, and the posted
Slack message link (or "nothing sent — no Failed/Feedback cases").
