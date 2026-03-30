---
name: mapper
description: >
  Create, update, and refactor Android Mappers using Test-Driven Development (TDD).
  Enforces writing test cases before production code, follows project mapper conventions
  (Class + @Inject constructor, operator fun invoke, single responsibility), and supports
  three modes: create, update, and refactor.
triggers:
  - /mapper
  - create mapper
  - new mapper
  - update mapper
  - refactor mapper
---

# Mapper TDD Skill

Create, update, and refactor Android Mappers using strict Test-Driven Development.

## Usage

```
/mapper create MyEntityMapper --module feature/myfeature
/mapper create MyEntityMapper --module feature/myfeature --suspend
/mapper create MyEntityMapper --module data --layer data
/mapper update MyEntityMapper --module feature/myfeature --change "handle nullable email field"
/mapper refactor MyEntityMapper --module feature/myfeature
```

## Arguments

| Argument | Description | Example |
|----------|-------------|---------|
| `create\|update\|refactor` | Mode of operation (required, first positional arg) | `create` |
| Mapper name | Name of the Mapper class (required, second positional arg) | `MyEntityMapper` |
| `--module <path>` | Module path relative to project root (required) | `--module feature/sync` |
| `--layer <layer>` | Target layer: `presentation` (default), `data`, `domain` | `--layer data` |
| `--suspend` | Use suspend operator fun invoke for async work | `--suspend` |
| `--change <desc>` | Description of what to change (for `update` mode) | `--change "add list overload"` |

---

## Execution Steps

### Mode: CREATE

#### Step 1 — Gather Requirements

1. Confirm the Mapper name, module path, and layer with the user.
2. Search the target module for existing mappers and tests to match local package structure and conventions:
   ```
   Glob: <module>/src/main/**/<layer>/**/mapper/**/*Mapper.kt
   Glob: <module>/src/test/**/*MapperTest.kt
   ```
3. Determine the package name by following existing module conventions.
4. Ask the user:
   - What is the input type (source)?
   - What is the output type (target)?
   - Are there dependent mappers to inject?
   - Does the mapper need to handle nullable input?
   - Is a list overload needed?
   - Does it require async work (`--suspend`)?

#### Step 2 — Define Test Cases (User Confirmation Gate)

Based on the gathered requirements, propose a list of test cases. Present them to the user in this format:

```
Proposed test cases for MyEntityMapper:

1. `test that id is mapped correctly`
2. `test that name is mapped to displayName`
3. `test that null input returns null`
4. `test that empty list returns empty list`
5. `test that list items are mapped individually`
...

Please confirm, modify, or add test cases before proceeding.
```

**Do NOT proceed until the user confirms the test cases.**

#### Step 3 — Generate Test File (RED)

Create the test file at `<module>/src/test/java/<package>/mapper/<Name>MapperTest.kt` following the Test Standards below. Include all confirmed test cases with proper structure but **failing implementations** (the tests should compile but fail — this is the RED phase).

#### Step 4 — Verify RED

Run the tests to confirm they fail:
```bash
./gradlew <module-gradle-path>:testDebugUnitTest --tests "<fully.qualified.TestClass>"
```

Report the results to the user. All new tests should fail (RED).

#### Step 5 — Generate Production Code (GREEN)

Create the mapper at `<module>/src/main/java/<package>/mapper/<Name>Mapper.kt`.

Follow the Mapper Standards below for all generated code.

#### Step 6 — Verify GREEN

Run the tests again:
```bash
./gradlew <module-gradle-path>:testDebugUnitTest --tests "<fully.qualified.TestClass>"
```

All tests should now pass (GREEN). If any test fails, fix the production code and re-run until green.

#### Step 7 — Refactor (if needed)

Review the generated code for any improvements while keeping tests green. Run tests after any refactoring.

---

### Mode: UPDATE

#### Step 1 — Locate Files

Find the existing mapper and its test:
```
Glob: **/<Name>Mapper.kt
Glob: **/<Name>MapperTest.kt
```

Read all located files to understand the current implementation.

#### Step 2 — Check for Legacy Patterns

Inspect the mapper for legacy patterns:
- `fun interface` with separate `Impl` class
- `typealias` function type
- `@ApplicationContext context: Context` injection
- Missing `@Inject constructor`
- Missing `internal` visibility (in feature/data modules)

If found:
- **Warn the user** that the mapper uses a legacy pattern.
- **Suggest running `/mapper refactor`** first to migrate to the standard pattern before adding new functionality.
- If the user wants to proceed without refactoring, continue but match the existing pattern for consistency within the file.

#### Step 3 — Define New Test Cases (User Confirmation Gate)

Based on the `--change` description, propose new test cases. Present them to the user for confirmation.

**Do NOT proceed until the user confirms.**

#### Step 4 — Add Tests (RED)

Add the new test cases to the existing test file. Run tests to verify the new tests fail while existing tests still pass.

#### Step 5 — Implement Change (GREEN)

Modify the mapper to make the new tests pass. Do not break existing tests.

#### Step 6 — Verify GREEN

Run the full test suite for the mapper to ensure all tests pass (both old and new).

---

### Mode: REFACTOR

#### Step 1 — Analyze

Locate and read the mapper and its test file. Identify:
- `fun interface` + `Impl` patterns that should be consolidated to `class` + `@Inject constructor`
- `typealias` patterns that should be converted to `class` + `@Inject constructor`
- `@ApplicationContext` / `Context` injection that should be removed (return `@StringRes Int` instead, or move string resolution to UI)
- Missing `@Inject constructor`
- Missing `internal` visibility in feature/data modules
- Missing `operator fun invoke` (e.g., using `fun map(...)` instead)
- Any other deviations from the Mapper Standards below

Present the analysis to the user.

#### Step 2 — Ensure Test Coverage

Check that existing tests cover the current behavior. If coverage is insufficient, propose additional test cases and get user confirmation before adding them. All existing tests must pass before refactoring begins.

#### Step 3 — Plan Refactoring

Present a step-by-step refactoring plan to the user. Each step should be a small, verifiable change.

#### Step 4 — Apply Incrementally

For each refactoring step:
1. Make the change
2. Run tests to verify nothing broke
3. If tests fail, fix or revert before proceeding

---

## Mapper Standards

See [mapper-conventions.md](./mapper-conventions.md) for all Mapper production code conventions.

---

## Test Standards

See [mapper-test-conventions.md](./mapper-test-conventions.md) for all Mapper test conventions.

---

## Guidelines

1. **TDD is mandatory** — tests are always written before production code. Never generate mapper code before the test file exists and fails.
2. **User confirmation gate** — always present proposed test cases and wait for explicit user approval before writing any test or production code.
3. **Module convention matching** — before generating code, search the target module for existing mappers and tests. Match the local package structure, naming patterns, and any module-specific conventions.
4. **Standard pattern** — always use `class` + `@Inject constructor()` + `operator fun invoke(...)`. Flag `fun interface`, `typealias`, and `Context` injection as legacy patterns for migration.
5. **Incremental verification** — run tests after every significant code change. Never assume code is correct without verification.
6. **Minimal changes** — when updating or refactoring, change only what is necessary. Do not refactor unrelated code.