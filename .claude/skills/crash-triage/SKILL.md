---
name: crash-triage
description: >
  Triages top crashes from Firebase Crashlytics by fetching crash reports,
  analyzing stack traces against the local codebase, correlating with recent
  git history, and generating a structured triage report. Optionally adds
  triage notes back to Crashlytics issues.
triggers:
  - /crash-triage
  - triage crashes
  - top crashes
  - crashlytics report
  - crash report
---

# Crash Triage

Fetch top crashes from Firebase Crashlytics, analyze stack traces against the local codebase,
correlate with recent git commits, and generate a structured triage report.

## Usage

```
/crash-triage                                # Triage top 5 crashes from the last 7 days
/crash-triage --top 10                       # Triage top 10 crashes
/crash-triage --type FATAL                   # Only fatal crashes
/crash-triage --type ANR --days 14           # ANRs from the last 14 days
/crash-triage --version "16.1"               # Filter by app version
/crash-triage --issue abc123def456           # Deep-dive a specific issue
/crash-triage --add-notes                    # Add triage notes to Crashlytics issues
/crash-triage --output ./triage-report.md    # Save report to a file
```

## Arguments

| Argument | Description | Example |
|----------|-------------|---------|
| `--type <type>` | Error type filter: `FATAL`, `NON_FATAL`, `ANR`, or `all` (default: `all`) | `--type FATAL` |
| `--days <number>` | Time range in days (default: 7, max: 90) | `--days 14` |
| `--top <number>` | Number of top issues to triage (default: 5, max: 25) | `--top 10` |
| `--version <string>` | Filter by app version display name (partial match supported) | `--version "16.1"` |
| `--add-notes` | Add a triage summary note to each Crashlytics issue | |
| `--issue <id>` | Triage a single specific issue by ID (skips top issues fetch) | `--issue abc123def456` |
| `--output <path>` | Save the full triage report to a file | `--output ./triage.md` |

## Execution Steps

### Step 0 — Resolve Firebase App ID

Resolve the Firebase project and Android app ID before any Crashlytics calls.

1. Call `mcp__plugin_firebase_firebase__firebase_get_environment` to verify authentication and get the active project.
2. Call `mcp__plugin_firebase_firebase__firebase_list_apps` to list all apps.
3. Select the Android app ID (platform = `ANDROID`). If multiple Android apps exist, ask the user which one to use.
4. Store the resolved `appId` for all subsequent Crashlytics calls.

**If not authenticated:** Halt and instruct the user to run `firebase login` (via `mcp__plugin_firebase_firebase__firebase_login` or the CLI).

### Step 1 — Fetch Top Issues

**If `--issue <id>` was provided:**
- Call `mcp__plugin_firebase_firebase__crashlytics_get_issue` with the provided `issueId`.
- Proceed to Step 2 with a single-issue list.

**Otherwise (default flow):**

1. Compute the time range:
   - `intervalStartTime` = ISO 8601 timestamp for (now minus `--days` days)
   - `intervalEndTime` = ISO 8601 timestamp for now
2. Build the filter:
   - If `--type` is not `all`: set `issueErrorTypes` to the array (e.g., `["FATAL"]`)
   - If `--type` is `all`: omit `issueErrorTypes` entirely
   - If `--version` is provided: first call `mcp__plugin_firebase_firebase__crashlytics_get_report` with `report: "topVersions"` to discover exact display names, then find the closest match and set `versionDisplayNames` to that value
   - Set `intervalStartTime` and `intervalEndTime`
3. Call `mcp__plugin_firebase_firebase__crashlytics_get_report` with:
   - `report: "topIssues"`
   - `pageSize`: the `--top` value (default 5)
   - `filter`: as constructed above
4. Parse the response to get a list of issues with: issue ID, title, subtitle, event count, impacted users count, and `sampleEvent` resource name.

### Step 2 — Fetch Stack Traces

**For top issues mode (default):**
1. Collect all `sampleEvent` resource names from Step 1.
2. Call `mcp__plugin_firebase_firebase__crashlytics_batch_get_events` with the `names` array containing all sample events. This fetches all stack traces in a single batched call.
3. Parse each event to extract: exception type, exception message, stack trace frames, device model, OS version, and app version.

**For single issue mode (`--issue`):**
1. Call `mcp__plugin_firebase_firebase__crashlytics_list_events` with:
   - `filter.issueId` set to the issue ID
   - `pageSize: 3` to get a few recent events
2. Parse the events as above.

### Step 3 — Parse Stack Traces and Identify Source Files

For each stack trace:

1. **Filter for project frames:** Scan all frames for class names containing `mega.privacy.android.`. These are the project-owned frames.

2. **Map package to source file path** using the project's module structure:

   | Package prefix | Source root(s) |
   |----------------|---------------|
   | `mega.privacy.android.app.*` | `app/src/main/java/`, `app/src/gms/java/` |
   | `mega.privacy.android.domain.*` | `domain/src/main/kotlin/` |
   | `mega.privacy.android.data.*` | `data/src/main/java/` |
   | `mega.privacy.android.feature.<name>.*` | `feature/<name>/*/src/main/java/`, `feature/<name>/*/src/main/kotlin/` |
   | `mega.privacy.android.shared.*` | `shared/*/src/main/java/`, `shared/*/src/main/kotlin/` |
   | `mega.privacy.android.core.*` | `core/*/src/main/java/`, `core/*/src/main/kotlin/` |

3. **Fallback:** If the deterministic path does not resolve, use `Glob` with pattern `**/<ClassName>.kt` (or `.java`) to find the file.

4. **Read source context:** For each identified file, read the crash line number +/- 20 lines to understand the crash context.

5. **Handle edge cases:**
   - **Obfuscated frames** (single-letter names, `$` suffixes with no readable class): Note "Stack trace may be from a release build without mapping file. Consider uploading ProGuard/R8 mapping."
   - **Native crashes** (C/C++ frames from `.so` files): Mark as "Native crash" and skip source file resolution.
   - **Coroutine machinery** (`kotlinx.coroutines.*`, `kotlin.coroutines.*`): Filter these from the "relevant frames" list but keep in the full trace.
   - **Inner classes / lambdas** (`MyClass$methodName$1`): Map to `MyClass.kt`.
   - **No project frames found:** List the top 3 external frames and mark as "External/SDK crash — no project source identified."

### Step 4 — Correlate with Git History

For each identified source file from Step 3:

1. Run `git log --oneline --since="<days> days ago" -n 10 -- <file_path>` to find recent commits.
2. Run `git log --format="%h %an %as %s" --since="<days> days ago" -n 5 -- <file_path>` to get author details.
3. If the stack trace includes a specific line number, run `git log -n 3 -L <line>,<line+10>:<file_path>` to find who last modified the exact crash location.
4. Collect: short commit hash, author name, date, and commit message.

**If no recent commits found:** Extend the search to 30 days. If still none, note "No recent changes — likely a pre-existing issue."

### Step 5 — Generate the Triage Report

Output a structured markdown report in the following format:

````markdown
# Crash Triage Report

**Generated:** YYYY-MM-DD HH:mm
**Time Range:** last N days (YYYY-MM-DD to YYYY-MM-DD)
**Filters:** type=FATAL | version=16.1 | (none)
**Issues Triaged:** N

## Summary

| # | Type | Title | Events | Users | Likely Culprit |
|---|------|-------|--------|-------|----------------|
| 1 | FATAL | NullPointerException in LoginViewModel.kt | 1,234 | 892 | @author (abc1234) |
| 2 | ANR | Input dispatching timed out | 567 | 321 | External/SDK |

---

## Issue 1: <Issue Title>

**Type:** FATAL | NON_FATAL | ANR
**Events:** N | **Users Affected:** N

### Stack Trace (Relevant Frames)

```
mega.privacy.android.app.presentation.login.LoginViewModel.onLoginClick(LoginViewModel.kt:142)
mega.privacy.android.domain.usecase.login.LoginUseCase.invoke(LoginUseCase.kt:38)
```

### Source Context

**File:** `app/src/main/java/.../LoginViewModel.kt` (line 142)
```kotlin
// Lines 122-162 shown
```

### Recent Git Activity

| Commit | Author | Date | Message |
|--------|--------|------|---------|
| abc1234 | John Doe | 2026-03-25 | AND-5678 Refactor login flow |
| def5678 | Jane Smith | 2026-03-20 | AND-5679 Fix null check |

### Likely Culprit

**Commit:** abc1234 by John Doe (2026-03-25)
**Reasoning:** Most recent change to the crash location (line 142) within the triage window.

### Suggested Investigation Steps

1. Check if the null check on `userSession` was removed in commit abc1234
2. Verify that `LoginUseCase` handles the case where session token is expired
3. Add a null safety check at LoginViewModel.kt:142

---

## Issue 2: ...

---

## External / SDK Crashes

| Type | Title | Events | Top Frame |
|------|-------|--------|-----------|
| FATAL | libsqlite.so crash | 234 | libsqlite.so+0x1234 |

These crashes occur in external libraries or native code. Consider updating the relevant dependency or filing an issue with the library maintainer.
````

### Step 6 — Add Crashlytics Notes (only if `--add-notes` was passed)

For each triaged issue:

1. Call `mcp__plugin_firebase_firebase__crashlytics_list_notes` to check for existing triage notes.
2. If a note starting with `[Auto-Triage` already exists from today, skip to avoid duplicates.
3. Call `mcp__plugin_firebase_firebase__crashlytics_create_note` with a concise summary:

```
[Auto-Triage YYYY-MM-DD]
Likely culprit: commit <hash> by <author> (<date>)
File: <path>:<line>
Recent changes: N commits in last <days> days
Action: <1-line suggested investigation step>
```

### Step 7 — Save Report (only if `--output` was passed)

1. If the path does not end in `.md`, append `.md`.
2. Write the full report to the specified path using the Write tool.
3. Confirm: "Triage report saved to `<path>`"

## Notes

- This skill can be run via `/schedule` for recurring automated triage (e.g., daily morning crash review).
- For large numbers of issues (`--top > 10`), expect longer execution times due to source file reads and git log lookups.
- The skill reads source files to understand crash context but does not modify any code.