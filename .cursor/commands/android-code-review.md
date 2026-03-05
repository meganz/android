# Android PR Code Review

Perform a comprehensive code review on the current Git repository's PR or specified code changes. Cover all review dimensions: architecture, Kotlin code quality, Android platform best practices, performance, security, and testability. Deliver feedback in a **constructive and respectful tone**.

## Usage

Parse optional parameters from the user's message after the command:

- **Default**: Review diff between current branch and `develop`
- `--branch <name>`: Review a specific branch
- `--base <branch>`: Base branch for comparison (default: `develop`)
- `--file <path>`: Review a single file
- `--focus <dimension>`: Focus on security, performance, arch, or testability
- `--severity <level>`: Only show issues at or above: critical, major, minor, suggestion
- `--output <path>`: Save report to a file

## Execution Steps

### Step 1 — Fetch Code Changes

```bash
# Default: current branch vs develop (or --base value)
git log ${BASE:-develop}...HEAD --oneline
git diff ${BASE:-develop}...HEAD --stat
git diff ${BASE:-develop}...HEAD

# If --branch specified: git diff ${BASE:-develop}...<branch-name>
# If --file specified: read the file directly
```

Reviews **committed** changes only. Uncommitted changes are not included.

### Step 2 — Analyze

Analyze each changed file using the dimensions below. For large PRs (> 500 lines), prioritize core business logic files.

### Step 3 — Generate Report

Output using the Standard Output Format below.

### Step 4 — Save (Optional)

If `--output <path>` was specified, save the full report markdown to that path.

---

## Tech Stack Conventions

| Area | Convention |
|------|------------|
| Language | Kotlin (no new Java files) |
| Min SDK | API 24 (Android 8.0) |
| Target SDK | API 36 |
| Architecture | MVVM + Clean Architecture |
| UI | Jetpack Compose (new), View system (legacy) |
| DI | Hilt |
| Async | Kotlin Coroutines + Flow |
| Local Storage | Room, DataStore, SQLCipher |
| Image Loading | Coil |
| Unit Testing | JUnit5 + MockK + Turbine |
| Build | Convention plugins, Version Catalogs |

---

## Review Dimensions & Checklists

### 1. Architecture & Design

- [ ] MVVM layering: UI → ViewModel → UseCase → Repository → Facade → Gateway
- [ ] Dependency direction correct (Repository must not depend on ViewModel)
- [ ] Each UseCase has single responsibility
- [ ] No cross-layer direct calls (e.g., UI accessing Repository directly)
- [ ] Interfaces appropriately abstracted
- [ ] Hilt annotations correct (`@HiltViewModel`, `@AndroidEntryPoint`, `@Module @InstallIn`)
- [ ] Gateway/Facade used to abstract SDK (not calling SDK directly from Repository)
- [ ] New repository implementations in data layer/package in `data` module, not `app`
- [ ] Repositories never inject use cases (inverts Clean Architecture)
- [ ] Repositories focused on data access; business logic in use cases

#### Modular Dependency Rules
- [ ] **`:feature:`** modules can depend on `:shared`, `:core`, and their own `:*-snowflakes`.
- [ ] **`:shared:`** modules can depend on `:core` and their own `:*-snowflakes`.
- [ ] **`:core:`** modules can only depend on other `:core` modules.
- [ ] **`Snowflake`** modules (ending in `-snowflakes` or `-snowflake-components`) can only depend on `:core`.

**Common issues**: ViewModel holding Context (memory leak), Repository in Fragment, SDK in Repository, Hilt module missing `@Singleton`, incorrect module dependencies, repository in app module, use case injected into repository, business logic in repository.

### 2. Kotlin Code Quality

- [ ] `val` preferred over `var`
- [ ] No unsafe `!!` non-null assertions
- [ ] Scope functions used appropriately
- [ ] Data classes for data-holding classes
- [ ] Sealed classes for state/result modeling
- [ ] Naming conventions, no duplication, functions ≤ 40 lines

**Common issues**: Force unwrap (`!!`), mutable `var` when `val` suffices.

### 3. Android Platform Best Practices

- [ ] No memory leak risk (Fragment holding View references beyond lifecycle)
- [ ] StateFlow collected with `repeatOnLifecycle(Lifecycle.State.STARTED)`
- [ ] No long-running work on main thread
- [ ] Composables: side effects in `LaunchedEffect`/`SideEffect`, not directly in body
- [ ] Use `Timber` for logging, not Android Logger

**Common issues**: Flow collected without lifecycle awareness, side effect in Composable body instead of `LaunchedEffect`.

### 4. Coroutines & Async

- [ ] Correct Dispatcher (injected by DI with annotation)
- [ ] Exceptions handled (`runCatching`)
- [ ] No coroutine leaks, no `GlobalScope`
- [ ] UI updates on main thread; `withContext` for IO

**Common issues**: UI update inside IO dispatcher, unhandled exceptions in `launch`.

### 5. Performance

- [ ] Compose: `remember`, `derivedStateOf` to avoid unnecessary recomposition
- [ ] Lists use `LazyColumn` (never full lists in ScrollView)
- [ ] Images with explicit size constraints
- [ ] No DB/network on main thread

### 6. Security

- [ ] No sensitive data in plaintext SharedPreferences (use EncryptedSharedPreferences)
- [ ] No sensitive data logged
- [ ] User inputs validated
- [ ] Sensitive fields encrypted with `EncryptData` before persistence
- [ ] No API keys or secrets committed

### 7. Testability

- [ ] ViewModel, UseCase, Repository have unit tests
- [ ] **Test method naming:** Use one of: `` `test that <method> <action>` `` (e.g. `` `test that init does not call loginToFolderUseCase` ``) or `` `test that <method> <action> when <cause>` `` (e.g. `` `test that init emits Loaded when login succeeds` ``)
- [ ] New logic has corresponding tests — no missing tests for new behavior
- [ ] Compose screens have UI tests
- [ ] `@ExtendWith(CoroutineMainDispatcherExtension::class)`, `@TestInstance(PER_CLASS)`
- [ ] `initViewModel()` helper with defaults
- [ ] Turbine for Flow testing

### 8. Code Style & Formatting

- [ ] Explicit imports at top of file — no inline fully qualified class names
- [ ] Package: `mega.privacy.android.feature.{feature}.{layer}`
- [ ] Naming: `{Feature}ViewModel`, `{Action}UseCase`, `Default{Feature}Repository`, `{Source}Mapper`
- [ ] Naming and comments are clear; no unclear naming or comments
- [ ] No leftover debug code or TODOs in production code
- [ ] **Extra or unused code after implementation switch** — When the change refactors or replaces an approach, check for leftover files, types, or dependencies that are no longer used and suggest removing or slimming them
- [ ] 4 spaces indentation (no tabs)
- [ ] KDoc on public APIs
- [ ] Strings in `shared_strings.xml`

---

## Severity Levels

| Level | Badge | Blocks Merge? |
|-------|-------|---------------|
| Critical | 🔴 | Yes |
| Major | 🟠 | Yes |
| Minor | 🟡 | No |
| Suggestion | 🔵 | No |

---

## Standard Output Format

```markdown
# PR Code Review Report

## Summary
- **Branch**: userid/JiraID-branch → develop
- **Files Changed**: X
- **Review Date**: YYYY-MM-DD HH:mm
- **Overall**: [One-sentence assessment]

## Issue Overview
| Severity | Count |
|----------|-------|
| 🔴 Critical | X |
| 🟠 Major | X |
| 🟡 Minor | X |
| 🔵 Suggestion | X |

---

## Detailed Findings

### `path/to/FileName.kt`

#### 🔴 [Critical] Issue Title
**Location**: Line XX
**Problem**: Clear description and why it's risky.
**Suggestion**:
```kotlin
// ❌ Current
...

// ✅ Suggested
...
```

---

## Highlights 👍
- `FileName.kt`: [Good practices worth acknowledging — at least one per review]

## Conclusion
> [Approved ✅ / Request Changes 🔄 / Needs Discussion 💬]
>
> Brief explanation. List blocking issues that must be fixed before merge (if any).
```

---

## Review Mindset

1. **Critique the code, not the person** — "this function" not "you wrote"
2. **Explain the why** — every issue includes a reason
3. **Always provide a solution** — suggest concrete fixes
4. **Acknowledge the good** — at least one positive per review
5. **Distinguish must-fix from nice-to-have** — Minor/Suggestion are not blocking
