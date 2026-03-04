# Generate MR Description

Generate a structured Merge Request description by comparing this branch against `develop`.

## Usage

```
/generate-mr-description                        # Generate MR description based on diff between current branch and develop
/generate-mr-description --output ./review.md   # Save MR description into a file
```

## Arguments

| Argument          | Description               | Example                |
|-------------------|---------------------------|------------------------|
| `--output <path>` | Save the report to a file | `--output ./review.md` |

## Steps

### 1. Gather branch changes

Run these git commands to get the commits and diff:

```bash
echo "=== COMMITS ===" && git log develop..HEAD --oneline && echo "=== DIFF ===" && git diff develop...HEAD
```

> `develop...HEAD` (three dots) shows only what this branch introduced since diverging from develop.

### 2. Analyze the changes

From the commits and diff, extract:
- **What** changed — use commit messages as semantic hints
- **Why** it changed — infer from commit messages and code context
- **Key Changes** — meaningful logic/behavior changes only. **Skip any test file additions or modifications** (files ending in `Test`, `Spec`, `_test`, `spec`, `.test.`, `.spec.`)
- **TODOs** — scan added lines (`+`) for `//TODO`, `// TODO`, `#TODO`, `FIXME`, or `HACK` comments

### 3. Write the description in this exact format

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

**Writing guidelines:**
- Use present tense ("Add", "Fix", "Refactor")
- Be concise — reviewers skim MR descriptions
- Don't pad with filler phrases
- Be honest about cons/risks — don't just list positives

### 4. Save to file (if `--output <path>` was passed)

If the user invoked this command with `--output <path>`:
- Append `.md` if the path doesn't already end in `.md`
- Create the directory if it doesn't exist
- Write the description to that file:

```bash
mkdir -p "$(dirname <path>)" && cat > <path> << 'EOF'
<description content>
EOF
```

- Confirm with: `✅ MR description saved to <resolved-path>`

If no `--output` was given, display the description in chat only.