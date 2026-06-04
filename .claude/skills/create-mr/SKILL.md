---
name: create-mr
description: >
  Creates a GitLab Merge Request for the current branch by auto-generating a structured
  description from the branch diff and pushing with GitLab push options.
triggers:
  - /create-mr
  - push mr
  - create mr
  - open mr
---

# Create MR

Generate a structured MR description and push the current branch to GitLab, creating a
Merge Request automatically.

## Usage

```
/create-mr                                   # Push current branch and auto-generate description
/create-mr --title "AND-1234 My fix"         # Override MR title
/create-mr --base develop                    # Override target branch (default: develop)
/create-mr --branch kg/AND-1234-my-feature   # Create and switch to a new branch (explicit name)
/create-mr --branch kg AND-1234 my feature   # Create and switch to a new branch (auto-slugified)
/create-mr --draft                           # Create as draft MR
/create-mr --squash                          # Force squash on merge (overrides default)
/create-mr --no-squash                       # Force no squash on merge (overrides default)
/create-mr --wip-label                       # Explicitly add "WIP" label (same as default)
/create-mr --no-wip-label                    # Skip the default "WIP" label
/create-mr --no-jira                         # Skip the post-MR Jira transition + comment
```

## Arguments

| Argument            | Description                                                                 | Example                              |
|---------------------|-----------------------------------------------------------------------------|--------------------------------------|
| `--title <title>`   | Override MR title (default: latest commit message)                          | `--title "AND-1234 Fix login"`       |
| `--base <branch>`   | Target branch for the MR (default: `develop`)                               | `--base main`                        |
| `--branch <name>`   | Create a new branch using an explicit full name                             | `--branch kg/AND-1234-my-feature`    |
| `--branch <prefix> <JIRA> [desc]` | Create a new branch, auto-slugified from prefix + JIRA + description | `--branch kg AND-1234 my feature` → `kg/AND-1234-my-feature` |
| `--draft`           | Create MR as draft                                                          |                                      |
| `--squash`          | Force squash on merge, regardless of target branch                          |                                      |
| `--no-squash`       | Force no squash on merge, regardless of target branch                       |                                      |
| `--wip-label`       | Explicitly add the "WIP" label                                              |                                      |
| `--no-wip-label`    | Skip adding the "WIP" label                                                 |                                      |
| `--no-jira`         | Skip the post-MR Jira transition + auto-comment                             |                                      |

## Steps

### Step 0 — Create new branch (only if `--branch` was passed)

If `--branch <value>` was provided, determine the branch name as follows:

- **If the first token contains a `/`** (e.g. `kg/AND-1234-my-feature`) → use it as-is as the branch name.
- **Otherwise** (e.g. `kg AND-1234 my feature`) → treat as `<prefix> <rest>`:
  - **Prefix** = first token (e.g. `kg`)
  - **Rest** = everything after the prefix (e.g. `AND-1234 my feature`)
  - **Branch name** = `<prefix>/<slug>` where slug = rest lowercased with spaces replaced by hyphens (e.g. `kg/AND-1234-my-feature`)

Then run:
```bash
git checkout -b "<branch-name>"
```

- If the command fails because the branch already exists, run `git checkout "<branch-name>"` instead and inform the user.
- Continue to Step 1 using this new branch as the current branch.

### Step 0.5 — Handle worktree (if running inside a git worktree)

Detect whether the current working directory is inside a git worktree by running:
```bash
git rev-parse --is-inside-work-tree && git worktree list --porcelain
```

If the output of `git worktree list` shows that the current directory is a linked worktree (i.e. it is not the main worktree):

1. Determine the branch name to use:
   - If `--branch` was passed, use that name (already resolved in Step 0).
   - Otherwise, use the current worktree branch name stripped of any worktree-specific prefix/suffix, or ask the user for a branch name.
2. From the **main worktree** directory, create the normal branch pointing at the same commit:
   ```bash
   git branch "<branch-name>" "<current-worktree-HEAD-commit>"
   ```
3. Switch the worktree to track that new normal branch:
   ```bash
   git checkout "<branch-name>"
   ```
4. Continue to Step 1 using this normal branch. All subsequent steps (push, MR creation) will use this branch.

This ensures worktree-internal branches (which are temporary) are never used as MR source branches.

### Step 1 — Ensure commits are GPG-signed

#### Sub-step A — Commit any uncommitted changes (signed)

Run:
```bash
git status --porcelain
```

If the output is non-empty (uncommitted changes exist):
1. Ask the user for a commit message before proceeding — do not auto-generate one silently.
2. Stage and commit using the user's default GPG key:
   ```bash
   git add -A
   git commit -S -m "<user-provided message>"
   ```
3. If the commit fails because no default GPG key is configured, surface the error and halt with instructions:
   ```bash
   # List available secret keys
   gpg --list-secret-keys --keyid-format=long

   # Set the signing key and enable auto-signing
   git config --global user.signingkey <KEY_ID>
   git config --global commit.gpgsign true
   ```
   Ask the user to configure their GPG key and then re-run `/create-mr`.

#### Sub-step B — Verify all branch commits are GPG-signed

Run:
```bash
git log develop..HEAD --pretty="format:%H %s %G?"
```

The `%G?` field reports signature status per commit:
- `G` — good signature
- `U` — good signature, unknown key
- `X` / `Y` / `R` — expired or revoked key (treat as warning, still proceed)
- `B` — bad signature (halt)
- `N` — no signature (halt)

If **any commit** shows `N` or `B`:
- List the offending commits (hash + subject) to the user.
- **Halt** — do not proceed to the next step.
- Instruct the user to re-sign all branch commits:
  ```bash
  git rebase --exec "git commit --amend --no-edit -S" develop
  ```
  After re-signing, ask the user to re-run `/create-mr`.

If all commits are signed (no `N` or `B`), continue to Step 2.

### Step 2 — Gather branch info

Run:
```bash
git branch --show-current
git log develop..HEAD --oneline
```

- Use the current branch name as the push target.
- Use the latest commit message as the default MR title (if `--title` was not provided).

### Step 3 — Generate MR description

Run:
```bash
echo "=== COMMITS ===" && git log develop..HEAD --oneline && echo "=== DIFF ===" && git diff develop...HEAD
```

Analyze the output and extract:
- **What** changed — use commit messages as semantic hints
- **Why** it changed — infer from commit messages and code context
- **Key Changes** — meaningful logic/behavior changes only; **skip test-only files** (`*Test.kt`, `*Spec.kt`, `_test`, `.spec`)
- **TODOs** — scan added lines (`+`) for `//TODO`, `// TODO`, `#TODO`, `FIXME`, or `HACK` markers

Write the description in this exact format:

```
#### Summary
<1-3 sentence overview of what this MR does and why. Explain how it differs from the current implementation>

#### Key Changes
- <change 1>
- <change 2>

#### Benefits
- <benefit 1>
- <benefit 2>

#### Cons / Risks (if any)
- <con or risk — omit this section entirely if none>

#### TODOs for Next MR
- <//TODO or FIXME items found, or "None">

#### Why are we making this change?

#### What features are impacted?

#### If the MR has more than 10 files, please provide a valid reason.

## Screenshot/Screen-recording comparisons

| Before | After |
|--------|-------|
|        |       |

## Resources

[Android MR Checklist](https://confluence.developers.mega.co.nz/display/MOB/Android+MR+Checklist)

## Gitlab MR shortcuts

- jenkins rebuild - Run build again
- deliver_qa - Send build to firebase

Documentation: [Android CI/CD Pipeline Commands](https://confluence.developers.mega.co.nz/pages/viewpage.action?pageId=37651416)

Closes <Jira Ticket Number>
```

Writing guidelines:
- Use present tense ("Add", "Fix", "Refactor")
- Be concise — reviewers skim MR descriptions
- Don't pad with filler phrases
- Be honest about cons/risks — don't just list positives

### Step 4 — Push and create MR

Run `git push` using the Bash tool with GitLab push options.

**Important:** GitLab push options do not support literal newline characters and will fail with `fatal: push options must not have new line characters`. Always build the description as a single-line string with `\n` (backslash-n) in place of every newline:

```bash
DESCRIPTION='#### Summary\n<text>\n\n#### Key Changes\n- item 1\n- item 2'
git push --set-upstream origin "<current branch>" \
  -o merge_request.create \
  -o "merge_request.title=<title>" \
  -o "merge_request.description=${DESCRIPTION}" \
  -o "merge_request.target_branch=<base>" \
  -o "merge_request.label=WIP"
```

Rules for building `DESCRIPTION`:
- Write the entire value on **one line** inside single quotes
- Replace every newline with the two-character sequence `\n`
- Escape any single quotes in the text as `'"'"'`

If `--draft` was passed, append `-o merge_request.draft` to the command.

Squash behaviour (in priority order):
1. If `--squash` was passed → append `-o merge_request.squash`
2. If `--no-squash` was passed → do not append squash
3. Otherwise (default) → append `-o merge_request.squash`

WIP label behaviour (in priority order):
1. If `--no-wip-label` was passed → do not append the label option
2. Otherwise (default, or `--wip-label` explicitly passed) → append `-o "merge_request.label=WIP"`

The `merge_request.label` push option can be repeated to add multiple labels; only `WIP` is added by this skill. The label must already exist in the GitLab project (it does for this repo).

### Step 5 — Confirm

- Display the generated MR description so the user can review it.
- Extract and display the MR URL from the `git push` output.

### Step 6 — Offer to update Jira (skip if `--no-jira`)

After the MR is successfully created, **ask the user** whether they want to
transition the Jira ticket and post an auto-comment. Do **not** update Jira
without an explicit yes — the MR is the source of truth and the user may not
want the ticket to move yet (e.g. draft MR, still iterating, stacked MR).

All Jira plumbing goes through `tools/jira/jira_cli.py` — refer
to the per-subcommand flows in `.claude/skills/jira/SKILL.md` rather than
re-implementing.

1. **Decide whether to prompt at all**:
   - If `--no-jira` was passed → skip silently.
   - Otherwise extract the Jira key:
     ```bash
     KEY=$(tools/jira/jira branch-ticket)
     ```
     If exit 1 (no key in branch name), skip with the note `Jira step
     skipped — no AND-\d+ in branch name.` Do not prompt.

2. **Look up current Jira status** before prompting, so the user can decide
   meaningfully:
   ```bash
   tools/jira/jira status "$KEY"
   ```
   If the CLI exits non-zero (auth, network), skip the prompt and tell the
   user they can run `/jira submit <KEY>` manually later. Do not block the
   MR on this.

3. **Prompt the user** using `AskUserQuestion`:

   - **Question**: `Update Jira ticket <KEY> (currently <current-status>)?`
   - **Options**:
     - `Transition + comment` — runs the full `/jira submit <KEY> --mr-url
       <MR_URL>` flow (auto-comment + transition to Tech QA / Code Review /
       In Review / Ready for Review — whichever the workflow exposes).
     - `Comment only` — runs `/jira submit <KEY> --mr-url <MR_URL>
       --no-transition` (post the auto-comment but leave status alone).
     - `Skip` — no Jira API calls.

4. **Act on the answer**:
   - `Transition + comment` → follow the `/jira submit` flow end-to-end.
   - `Comment only` → run only the comment-posting steps from `/jira submit`.
   - `Skip` → no Jira API calls. Note in the final summary.

5. **Check Test Instructions** (only if the user did NOT pick `Skip` in
   step 3, and the Jira lookup in step 2 succeeded).

   **Follow the `/jira test-instructions <KEY>` flow defined in
   `.claude/skills/jira/SKILL.md` § "/jira test-instructions" verbatim** —
   do not re-implement the field discovery, read, classify-diff rc handling,
   or `--allow-empty` logic here. Future updates to that flow (exit-code
   contract, generation template, prompt wording) live there as the single
   source of truth; this step exists only to anchor the test-instructions
   sub-step to the same Jira ticket and propagate its final outcome into
   the MR-creation summary below.

   This step is independent from the transition decision — even if the user
   chose `Comment only` above, still offer to populate Test Instructions.

   On failure: same policy as the transition step — surface the error, do
   NOT fail the MR.

6. **Failure handling**: if any Jira step fails after the user opted in,
   surface the error but do **not** treat it as a failure of `create-mr` —
   the MR was created successfully. Tell the user they can re-run the
   relevant `/jira ...` command manually once the issue is resolved.

7. **Report**: append lines to the final summary reflecting what actually
   happened. Examples:

   ```
   Jira:              AND-1234 → Tech QA (comment posted)
   Test Instruction:  updated (was empty)
   ```

   ```
   Jira:              AND-1234 — comment posted (status unchanged)
   Test Instruction:  skipped (already populated)
   ```

   ```
   Jira:              skipped (user declined)
   Test Instruction:  skipped (user declined)
   ```

   ```
   Jira:              skipped (no AND-\d+ in branch)
   ```
