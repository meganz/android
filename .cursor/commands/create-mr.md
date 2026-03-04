# Create MR

Generate a structured MR description and push the current branch to GitLab, creating a Merge Request automatically.

## Usage

Parse optional parameters from the user's message after the command:

- **Default**: Push current branch and auto-generate description
- `--title "AND-1234 My fix"`: Override MR title (default: latest commit message)
- `--base develop`: Override target branch (default: `develop`)
- `--branch kg/AND-1234-my-feature`: Create and switch to a new branch first
- `--draft`: Create as draft MR
- `--squash`: Force squash on merge (overrides default)
- `--no-squash`: Force no squash on merge (overrides default)

## Execution Steps

### Step 0 — Create new branch (only if `--branch` was passed)

If `--branch <name>` was provided, run:

```bash
git checkout -b "<branch-name>"
```

- If the command fails because the branch already exists, run `git checkout "<branch-name>"` instead and inform the user.
- Continue to Step 1 using this new branch as the current branch.

### Step 1 — Gather branch info

Run (replace `develop` with `--base` value if provided):

```bash
git branch --show-current
git log develop..HEAD --oneline
```

- Use the current branch name as the push target.
- Use the latest commit message as the default MR title (if `--title` was not provided).
- Use the base branch (`--base` or `develop`) for all subsequent git commands and push options.

### Step 2 — Generate MR description

Run:

```bash
echo "=== COMMITS ===" && git log develop..HEAD --oneline && echo "=== DIFF ===" && git diff develop...HEAD
```

(Use the base branch from Step 1 instead of `develop` if `--base` was specified.)

Analyze the output and extract:
- **What** changed — use commit messages as semantic hints
- **Why** it changed — infer from commit messages and code context
- **Key Changes** — meaningful logic/behavior changes only; **skip test-only files** (`*Test.kt`, `*Spec.kt`, `_test`, `.spec`)
- **TODOs** — scan added lines (`+`) for `//TODO`, `// TODO`, `#TODO`, `FIXME`, or `HACK` markers

Write the description in this exact format:

```markdown
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

**Writing guidelines:**
- Use present tense ("Add", "Fix", "Refactor")
- Be concise — reviewers skim MR descriptions
- Don't pad with filler phrases
- Be honest about cons/risks — don't just list positives

### Step 3 — Push and create MR

Run `git push` with GitLab push options. GitLab push options do not allow newline characters — convert newlines to `\n` before passing:

1. Write the generated description to a temp file (e.g. `/tmp/mr-description.md`).
2. Replace newlines with `\n` and pass to `git push`:

```bash
DESCRIPTION=$(sed ':a;N;$!ba;s/\n/\\n/g' /tmp/mr-description.md)
git push --set-upstream origin "<current branch>" \
  -o merge_request.create \
  -o "merge_request.title=<title>" \
  -o "merge_request.description=${DESCRIPTION}" \
  -o "merge_request.target_branch=<base>"
```

If `--draft` was passed, append `-o merge_request.draft` to the command.

Squash behaviour (in priority order):
1. If `--squash` was passed → append `-o merge_request.squash`
2. If `--no-squash` was passed → do not append squash
3. Otherwise (default) → append `-o merge_request.squash`

### Step 4 — Confirm

- Display the generated MR description so the user can review it.
- Extract and display the MR URL from the `git push` output.
