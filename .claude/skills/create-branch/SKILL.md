---
name: create-branch
description: >
  Create a new git branch for a Jira ticket using the project's
  `<user>/<JIRA>-<slug>` naming convention, then transition the Jira ticket
  to "In Progress" via the `/jira start` flow. Works in the main checkout
  and inside an existing worktree.
triggers:
  - /create-branch
  - create branch
  - new branch
---

# Create Branch

Create a new git branch from `develop` and link the work to its Jira ticket
by transitioning the ticket to **In Progress** in one step. All deterministic
logic — branch-name building, ticket extraction, slugification — is in
`tools/jira/jira_cli.py`. This SKILL.md just orchestrates.

## Usage

```
/create-branch AND-1234 fix login crash
  # → branch: jdoe/AND-1234-fix-login-crash
  # → AND-1234 transitioned to In Progress

/create-branch AND-1234-fix-login-crash            # combined arg works too — ticket is auto-extracted, not duplicated in slug
/create-branch jdoe/AND-1234-fix-login-crash       # explicit full branch name (passthrough)
/create-branch AND-1234                             # description optional
/create-branch --no-jira AND-1234 some change      # skip the Jira transition
/create-branch --base main AND-1234 hotfix         # base off main, not develop
/create-branch --no-pull AND-1234 quick fix        # skip git fetch/pull of base
```

## Arguments

| Argument            | Description                                                          |
|---------------------|----------------------------------------------------------------------|
| `<JIRA-ID>` `<desc>`| Jira key + optional description (multiple words allowed)             |
| `<full-branch>`     | A fully-qualified branch name containing `/` is used as-is           |
| `--base <branch>`   | Base branch (default: `develop`)                                     |
| `--no-jira`         | Skip the `/jira start` transition (still creates the branch)         |
| `--no-pull`         | Skip `git fetch && git pull` of the base before branching            |

## Steps

### Step 1 — Build the branch name

**First strip this skill's own flags** (`--base <branch>`, `--no-jira`,
`--no-pull`) from the args — including the value that follows `--base`. Only
the Jira key + the free-form description words should reach the CLI:

```bash
# e.g. user typed: --no-pull AND-1234 quick fix --base main
# pass ONLY:        AND-1234 quick fix
tools/jira/jira branch-name <ticket + description words>
```

This matters because `branch-name` slugifies whatever it receives. The CLI
defensively drops any leftover `--`-prefixed token, but it cannot know that
the bare `main` after a stripped `--base` is a flag value — so leaving
`--base main` in would yield `...-main` in the slug. Strip flags here.

It returns the full branch name (`<prefix>/<TICKET>-<slug>`) or exits 1 if
no ticket could be parsed. Prefix comes from `git config user.email`'s local
part (`jdoe@mega.co.nz` → `jdoe`).

Some epics use a different branch convention (e.g. an extra component
segment like `<user>/<JIRA>-<component>NN-<desc>`). The CLI does NOT know
about those; if the ticket falls under such an epic, pass the full branch
name as the first arg so the CLI hits its passthrough path.

### Step 2 — Extract the Jira key (for Step 5)

```bash
tools/jira/jira branch-ticket
```

But that reads the *current* branch, not the new one. For `/create-branch`
we already have the new args — extract the key from them with the same
regex (`AND-\d+`, case-insensitive, first match). The CLI's `branch-ticket`
is only useful *after* the branch has been created. If no key is found:
- With `--no-jira` → continue (no Jira step will run).
- Without `--no-jira` → halt and ask the user.

### Step 3 — Detect worktree context

```bash
git rev-parse --is-inside-work-tree
git worktree list --porcelain
```

This skill does NOT create worktrees — that's `EnterWorktree`'s job. It
just branches in whatever checkout/worktree the user is in.

### Step 4 — Refresh base + create branch

Unless `--no-pull` was passed:

```bash
git fetch origin
git checkout <base>          # default: develop
git pull --ff-only origin <base>
```

If `git checkout` fails because of uncommitted changes that would be
overwritten, **halt** and surface the error. Do NOT discard the user's work.

Then:

```bash
git checkout -b <branch-name>
```

If the branch already exists, ask the user whether to switch to it or pick
a different name. Do not force-overwrite.

### Step 5 — Transition Jira ticket (skip if `--no-jira`)

Invoke the `/jira start` flow from `.claude/skills/jira/SKILL.md`. Pass the
key explicitly. If transition fails, report the failure but do NOT roll back
the branch — the branch was created successfully and the user can re-run
`/jira start <KEY>` later.

### Step 6 — Report

```
Branch created:   <branch-name>
Base:             <base>
Jira:             <KEY> → In Progress   (or: skipped / failed: <reason>)
```

## Notes

- This skill does **not** push the branch. Use `/create-mr` when ready.
- This skill does **not** create worktrees. Use `EnterWorktree` first if
  you want isolation.
