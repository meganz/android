---
name: fix-jira-bug
description: >
  Use when the developer asks to fix a Jira bug given a ticket ID or Jira link
  (e.g. "/fix-jira-bug AND-12345", "fix AND-12345"). End-to-end workflow:
  fetch the ticket, create a git worktree from latest develop, run multi-agent
  root cause analysis, get the fix plan approved by the developer, implement
  with a regression test, build and verify on a device via mobile-mcp, then
  commit and create a Merge Request.
triggers:
  - /fix-jira-bug
  - fix jira bug
  - fix bug AND-
---

# Fix Jira Bug

End-to-end orchestration for fixing a bug reported in Jira. This skill composes
existing building blocks (`/jira`, `/create-mr`, atlassian MCP, mobile-mcp) and
adds worktree setup, multi-agent root cause analysis, and on-device verification.

## Usage

```
/fix-jira-bug AND-12345
/fix-jira-bug https://jira.example.com/browse/AND-12345
```

## Iron Rules

These override any urge to move fast. Violating one of them invalidates the fix.

1. **No speculative fixes.** Every root cause conclusion must be backed by an
   evidence chain in code: file:line references, the call path, and an
   explanation of the exact trigger condition. "This change might fix it" is
   not a fix — it is a guess.
2. **Ask when uncertain.** If the root cause cannot be confirmed, or the fix
   approach is risky or has multiple plausible options, stop and confirm with
   the developer via AskUserQuestion before proceeding. Never silently pick
   the option that "looks reasonable".
3. **Developer approves the plan.** No production code is written before the
   developer approves the fix plan (via plan mode approval).
4. **Verified means verified on device.** The bug's reproduction steps must be
   walked on a real device or emulator before committing. Unit tests passing
   is necessary but not sufficient.

## Quick Reference

| Step | Action | Tools / skills | Developer confirmation? |
|------|--------|----------------|-------------------------|
| 0 | Parse ticket key | regex `AND-\d+` | only if parsing fails |
| 1 | Fetch ticket | `mcp__atlassian__jira_get_issue` (+ images/attachments) | yes, if repro steps unclear |
| 2 | Create worktree from develop | `tools/jira/jira branch-name`, `git worktree add` | ask before `/jira start` |
| 3 | Root cause analysis | plan mode + parallel agents or Workflow | yes, if uncertain |
| 4 | Fix plan review | ExitPlanMode | yes — approval gate |
| 5 | Implement fix + regression test | Edit/Write, project TDD conventions | no |
| 6 | Build + unit tests | `./gradlew :app:assembleGmsDebug`, `:module:testDebugUnitTest` | no |
| 7 | Verify on device | mobile-mcp | device choice; repro steps if unclear |
| 8 | Commit | git (GPG-signed) | no |
| 9 | Create MR + Jira transition | `/create-mr`, `/jira submit` | ask before `/jira submit` |
| 10 | Cleanup worktree (optional) | `git worktree remove` | only after merge/abandon |

## Workflow

### Step 0 — Parse input

Extract the ticket key with regex `AND-\d+` from the argument (works for both a
bare key and a Jira URL). If no key can be extracted, report the problem and
ask the developer for the ticket.

### Step 1 — Fetch and understand the ticket

1. Fetch the issue with `mcp__atlassian__jira_get_issue` (summary, description,
   affected version, comments).
2. If the ticket has screenshots or attachments that help understand the bug,
   fetch them with `mcp__atlassian__jira_get_issue_images` /
   `mcp__atlassian__jira_download_attachments`.
3. Present a short summary to the developer: observed behavior, reproduction
   steps, expected behavior, affected version/devices.
4. **If reproduction steps are missing, ambiguous, or incomplete, ask the
   developer NOW** (AskUserQuestion) — do not enter analysis with a fuzzy
   understanding of what the bug actually is.

### Step 2 — Create a worktree from latest develop

1. Build the branch name deterministically:
   `tools/jira/jira branch-name <KEY> <ticket summary>`. This command outputs
   the **full** branch name including the user prefix (e.g.
   `rsh/AND-12345-fix-login-crash`). Capture its output and use it as-is — do
   not construct the name manually or type a literal `<user>` string.
2. The worktree directory is the branch name **without the user prefix**
   (the part after the `/`), placed in the parent directory of the main
   checkout:

   ```bash
   git fetch origin develop
   BRANCH="$(tools/jira/jira branch-name <KEY> <ticket summary>)"   # e.g. rsh/AND-12345-fix-login-crash
   DIR="${BRANCH#*/}"                                               # strip user prefix → AND-12345-fix-login-crash
   # The project's bash guard blocks '..' in paths — resolve an absolute path first
   PARENT_DIR="$(dirname "$(git rev-parse --show-toplevel)")"
   git worktree add -b "$BRANCH" "$PARENT_DIR/$DIR" origin/develop
   cd "$PARENT_DIR/$DIR"
   git submodule update --init --recursive    # repo contains the sdk submodule
   ```

3. Copy `local.properties` from the main checkout into the worktree using the
   **Read + Write tools** (it is untracked but required for builds; the bash
   guard blocks `cp`). If the build later fails with SDK path errors, the copy
   may be incomplete — ask the developer to copy the file manually to confirm.
4. All subsequent work happens inside this worktree.
5. Ask the developer whether to run `/jira start <KEY>` (transition to
   In Progress). Do not transition automatically.

Note: this creates a **properly named branch** (not a temporary
EnterWorktree-style branch), so it can later be pushed directly as the MR
source branch.

### Step 3 — Multi-agent root cause analysis (plan mode)

Enter plan mode (EnterPlanMode) for read-only analysis. First triage the bug's
complexity, then pick the orchestration mode:

**Simple bug** (clear stack trace, single module, obvious change site):
dispatch 2–3 subagents **in parallel**, each with a distinct angle:

- *Code path tracing* — follow the stack trace / feature entry point through
  the data flow to the failure site.
- *Git history archaeology* — `git log -L <line-range>:<file>`, recent changes
  to the involved files, find the commit that introduced the regression.
- *Reproduction condition analysis* — what state, timing, or input triggers
  the failure; under what conditions it does NOT occur.

**Complex bug** (cross-module, concurrency/timing, multiple plausible causes):
use the Workflow tool to orchestrate:

1. Parallel finders, each from a different perspective (code path, git
   history, concurrency/lifecycle, state management).
2. **Adversarial verification** — for each candidate root cause, an
   independent agent attempts to refute it against the actual code.
3. Synthesis — a single confirmed root cause with its evidence chain, plus
   the proposed fix.

Whichever mode is used, the output of this step must contain:

- The confirmed root cause with evidence (file:line, call path, trigger
  condition).
- The proposed fix (what changes, where, and why this is the right layer).
- The regression test plan.
- The on-device verification plan (concrete repro steps).

If the analysis cannot converge on a confirmed root cause, or several fixes
are equally defensible, present the findings and **ask the developer** —
do not pick one unilaterally (Iron Rule 2).

### Step 4 — Developer reviews the fix plan

Submit the fix plan via ExitPlanMode for the developer's approval. Only start
changing code after approval. If the developer requests changes, revise and
resubmit.

### Step 5 — Implement the fix with a regression test

1. **Test first**: write a unit test that covers the bug and fails against the
   unfixed code. Follow the project's test conventions (JUnit 5, Mockito,
   Truth, Turbine; naming `test that <method> <action> when <cause>`).
2. Implement the fix so the test passes.
3. Follow the relevant convention docs when touching ViewModels, UseCases, or
   Mappers (see `.claude/skills/viewmodel|usecase|mapper/`).
4. Keep code comments concise and general — no Jira ticket IDs in comments.

### Step 6 — Build and run unit tests

```bash
./gradlew :app:assembleGmsDebug
./gradlew :<edited-module>:testDebugUnitTest   # for every module touched
```

A cold worktree build has no build cache and is slow — run with an extended
timeout (10 min) and/or `run_in_background`.

Report failures honestly with the output, fix them, and re-run. Never skip or
hand-wave a failing build/test.

### Step 7 — Verify on a device (mobile-mcp)

1. List devices with `mcp__mobile-mcp__mobile_list_available_devices`. If
   exactly one device is connected, use it; if several, ask the developer
   which one to use.
2. Install the freshly built APK from `app/build/outputs/apk/gms/debug/`
   (`mcp__mobile-mcp__mobile_install_app`).
3. Walk the reproduction steps from the ticket step by step (launch, tap,
   navigate), taking screenshots as evidence.
4. **If the repro steps cannot be followed or are unclear in practice, ask the
   developer to confirm the exact steps** — do not guess or skip steps.
5. Success criterion: the behavior that reproduced before the fix no longer
   occurs. Show the developer the verification result with screenshots.

### Step 8 — Commit

Once device verification passes, commit on the worktree branch:

- Message style follows the repo convention: `AND-12345 <imperative summary>`
  (see recent commits for tone).
- Verify commits are GPG-signed:
  `git log develop..HEAD --pretty="format:%H %s %G?"` — halt on `N` or `B`.

### Step 9 — Create the MR and offer Jira transition

1. Delegate to the `/create-mr` skill (target branch `develop`). The worktree
   branch is a properly named branch and can be pushed directly.
2. After the MR is created, **ask** the developer whether to run
   `/jira submit <KEY> --mr-url <url>` (transition to Tech QA + QA comment).
   Do not transition automatically.

### Step 10 — Cleanup the worktree (optional)

Worktrees accumulate and clutter `git worktree list` / consume disk. Once the
MR is merged (or the work is abandoned), offer to remove the worktree. Do **not**
remove it while the MR is still open or unmerged.

```bash
git worktree remove "$PARENT_DIR/$DIR"   # the directory created in Step 2
```

## Red Flags — STOP and Ask the Developer

If you catch yourself thinking any of these, stop and confirm:

- "This change *might* fix it" — you have a guess, not a root cause.
- "Let me just try this and see if it helps" — speculative fix.
- "The repro steps are *probably* something like..." — unconfirmed repro.
- "Both fixes look fine, I'll go with the simpler one" — developer's call.
- "Unit tests pass, device verification is probably unnecessary" — it is not.
- "While I'm here, I should also fix this unrelated issue" — scope creep; file
  a separate ticket and keep this fix focused.

## Common Mistakes

| Mistake | Fix |
|---------|-----|
| Worktree build fails with missing SDK/paths | Run `git submodule update --init --recursive` and copy `local.properties` right after `git worktree add` |
| Pushing a temporary worktree branch as MR source | This skill creates a named branch up front — push that one; never push EnterWorktree-internal branches |
| Committing before device verification | Step 7 gates Step 8 — always verify the repro steps on device first |
| Jira ticket IDs in code/test comments | Keep comments general; the ticket ID belongs in the commit message and MR only |
| Hardcoding Jira transition names | Always discover transitions at runtime via `tools/jira/jira transitions <KEY>` (handled by `/jira`) |
