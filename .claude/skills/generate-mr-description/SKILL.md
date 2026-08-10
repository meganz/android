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
- <one key change in a single plain-english sentence>
- <another key change in a single plain-english sentence>

#### Key Changes
- <one meaningful change, explained in plain english>
  - <optional sub-bullet breaking down complex logic, if it helps the reader>
- <another meaningful change, explained in plain english>

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
- **Summary** must be bullet points, never a paragraph. Give each key change its own bullet, and keep each bullet to a single sentence. Don't combine multiple changes into one bullet.
- Write Summary bullets in plain english that's easy to read and grasp at a glance — avoid jargon-heavy or run-on phrasing.
- **Key Changes** must be bullet points. Explain each change in simple, plain english.
- Leave trivial or incidental changes out of Key Changes — e.g. test cases, code formatting, renames, comment tweaks. Only list changes that affect behavior or logic a reviewer needs to know.
- When a change involves complex logic, break it down with indented sub-bullets, but only when doing so makes it easier for the reader to follow.
- Visualize a change when a picture beats prose. Under the relevant bullet, add:
  - a **Mermaid diagram** for flows, sequences, state machines, or class/structure relationships (e.g. ```` ```mermaid ```` with `flowchart`, `sequenceDiagram`, `stateDiagram-v2`, or `classDiagram`) — GitLab renders Mermaid in MR descriptions natively. For example, a state transition change:

    ```mermaid
    stateDiagram-v2
        Idle --> Loading : fetch()
        Loading --> Success : data received
        Loading --> Error : exception
    ```

  - a **table** when comparing options or listing values across several dimensions (e.g. before/after, per-case behavior).
  - Only add a visual when it genuinely makes the change clearer — never decorate a simple change with a diagram or table.
  - Indent the diagram or table to match the hierarchy level of the bullet it belongs to. A visual placed under a sub-bullet must be indented to align with that sub-bullet's text (e.g. the ```` ```mermaid ```` fence, every diagram line, the closing fence, and table rows all start at the sub-bullet's indent column), so the markdown nests the visual inside the bullet instead of breaking out to the top level.

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