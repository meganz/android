---
name: usecase
description: >
  Create, update, and refactor Android Use Cases using Test-Driven Development (TDD).
  Enforces writing test cases before production code, follows project use case conventions
  (@Inject constructor, operator fun invoke, single responsibility), and supports three modes:
  create, update, and refactor.
triggers:
  - /usecase
  - create usecase
  - new usecase
  - update usecase
  - refactor usecase
---

# UseCase TDD Skill

Create, update, and refactor Android Use Cases using strict Test-Driven Development.

## Usage

```
/usecase create GetPricingUseCase --package billing
/usecase create MonitorNodeUpdatesUseCase --package node
/usecase update GetPricingUseCase --change "add caching logic"
/usecase refactor GetPricingUseCase
```

## Arguments

| Argument | Description | Example |
|----------|-------------|---------|
| `create\|update\|refactor` | Mode of operation (required, first positional arg) | `create` |
| UseCase name | Name of the UseCase class (required, second positional arg) | `GetPricingUseCase` |
| `--package <name>` | Feature subpackage under `usecase/` (for `create` mode, see Package Resolution below) | `--package billing` |
| `--change <desc>` | Description of what to change (for `update` mode) | `--change "add caching logic"` |

**Module**: All use cases are created in the `domain` module. This is not configurable.

---

## Execution Steps

### Mode: CREATE

#### Step 1 — Gather Requirements

1. Confirm the UseCase name and feature description with the user.
2. **Resolve the target package** (see Package Resolution below).
3. Search the target package for existing use cases and tests to match local conventions:
   ```
   Glob: domain/src/main/kotlin/**/usecase/<package>/*UseCase.kt
   Glob: domain/src/test/kotlin/**/usecase/<package>/*UseCaseTest.kt
   ```
4. Ask the user:
   - What operation does this use case perform?
   - What repositories or other use cases does it depend on?
   - Is it suspend (one-shot), Flow-returning (monitoring), or synchronous (pure computation)?
   - What are the inputs and return type?

#### Step 2 — Define Test Cases (User Confirmation Gate)

Based on the gathered requirements, propose a list of test cases. Present them to the user in this format:

```
Proposed test cases for GetPricingUseCase:

1. `test that invoke returns pricing from repository`
2. `test that invoke throws exception when repository fails`
3. `test that invoke passes correct parameters to repository`
...

Please confirm, modify, or add test cases before proceeding.
```

**Do NOT proceed until the user confirms the test cases.**

#### Step 3 — Generate Test File

Create the test file at the appropriate test location following the Test Standards below. Include all confirmed test cases with proper structure but **failing implementations** (the tests should compile but fail — this is the RED phase).

#### Step 4 — Verify RED

Run the tests to confirm they fail:
```bash
./gradlew <module-gradle-path>:testDebugUnitTest --tests "<fully.qualified.TestClass>"
```

Report the results to the user. All new tests should fail (RED).

#### Step 5 — Generate Production Code

Create the use case file at the appropriate location. Follow the UseCase Standards below.

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

Find the existing use case and its test:
```
Glob: **/<Name>UseCase.kt
Glob: **/<Name>UseCaseTest.kt
```

Read all located files to understand the current implementation.

#### Step 2 — Check for Legacy Patterns

Inspect the use case for legacy patterns:
- `fun interface` SAM pattern
- `interface` + `Default*` implementation class
- Missing `@Inject constructor`

If found:
- **Warn the user** that the use case uses a legacy pattern.
- **Suggest running `/usecase refactor`** first to migrate to the concrete class pattern before adding new functionality.
- If the user wants to proceed without refactoring, continue but maintain consistency within the file.

#### Step 3 — Define New Test Cases (User Confirmation Gate)

Based on the `--change` description, propose new test cases. Present them to the user for confirmation.

**Do NOT proceed until the user confirms.**

#### Step 4 — Add Tests (RED)

Add the new test cases to the existing test file. Run tests to verify the new tests fail while existing tests still pass.

#### Step 5 — Implement Change (GREEN)

Modify the use case to make the new tests pass. Do not break existing tests.

#### Step 6 — Verify GREEN

Run the full test suite for the use case to ensure all tests pass (both old and new).

---

### Mode: REFACTOR

#### Step 1 — Analyze

Locate and read the use case and test file. Identify:
- Legacy `fun interface` SAM pattern that should migrate to concrete class
- `interface` + `Default*` implementation that should be consolidated into a single class
- Direct data layer dependencies that should be replaced with repository interfaces
- Multiple public methods (violates single responsibility)
- Mutable state in the use case
- Any other deviations from the UseCase Standards below

Present the analysis to the user.

#### Step 2 — Ensure Test Coverage

Check that existing tests cover the current behavior. If coverage is insufficient, propose additional test cases and get user confirmation before adding them. All existing tests must pass before refactoring begins.

#### Step 3 — Plan Refactoring

Present a step-by-step refactoring plan to the user. For interface-to-class migration:
1. Create the new concrete class with `@Inject constructor`
2. Update all injection sites (ViewModels, other use cases) to use the concrete class
3. Remove the old interface and Default* implementation
4. Remove the Hilt `@Binds` module entry
5. Update tests to use the concrete class directly

#### Step 4 — Apply Incrementally

For each refactoring step:
1. Make the change
2. Run tests to verify nothing broke
3. If tests fail, fix or revert before proceeding

---

## Package Resolution

Use cases live in feature subpackages under `domain/src/main/kotlin/mega/privacy/android/domain/usecase/{package}/`. **Never place new use cases directly in the root `usecase/` package** — the flat root structure is legacy and should not grow further.

### Resolution order

1. **Explicit `--package`**: If the user provides `--package billing`, use `usecase/billing/`.
2. **Infer from dependencies**: If the use case's primary repository dependency maps to an existing subpackage, use that. For example, a use case injecting `NodeRepository` likely belongs in `usecase/node/`.
3. **Infer from name**: Match the use case name against existing subpackages. For example, `GetChatMessageUseCase` → `usecase/chat/`, `MonitorTransferEventsUseCase` → `usecase/transfers/`.
4. **Search for related use cases**: If the use case is closely related to existing use cases, place it alongside them:
   ```
   Glob: domain/src/main/kotlin/**/usecase/**/*{keyword}*UseCase.kt
   ```
5. **Ask the user**: If no clear match is found, list the most likely candidate subpackages and ask the user to choose or name a new one.

### Existing subpackages (reference)

When inferring a package, check what already exists:
```
Glob: domain/src/main/kotlin/mega/privacy/android/domain/usecase/*/
```

Common subpackages include: `account`, `auth`, `billing`, `call`, `camerauploads`, `chat`, `contact`, `file`, `login`, `meeting`, `node`, `notifications`, `offline`, `photos`, `search`, `setting`, `shares`, `transfers`, `user`, `video`, and many more.

### Test file placement

Test files mirror the production package:
- Production: `domain/src/main/kotlin/mega/privacy/android/domain/usecase/{package}/{Name}UseCase.kt`
- Test: `domain/src/test/kotlin/mega/privacy/android/domain/usecase/{package}/{Name}UseCaseTest.kt`

---

## UseCase Standards

See [usecase-conventions.md](./usecase-conventions.md) for all UseCase production code conventions.

---

## Test Standards

See [usecase-test-conventions.md](./usecase-test-conventions.md) for all UseCase test conventions.

---

## Guidelines

1. **TDD is mandatory** — tests are always written before production code. Never generate UseCase code before the test file exists and fails.
2. **User confirmation gate** — always present proposed test cases and wait for explicit user approval before writing any test or production code.
3. **Domain module only** — all use cases are created in the `domain` module. Do not create use cases in feature modules.
4. **Package intelligence** — never place new use cases in the flat root `usecase/` package. Always resolve a feature subpackage using the Package Resolution steps. When in doubt, ask the user.
5. **Single concrete class pattern** — always use a concrete class with `@Inject constructor` and `operator fun invoke`. Legacy `fun interface` and `interface` + `Default*` patterns must be flagged for migration.
6. **Incremental verification** — run tests after every significant code change. Never assume code is correct without verification.
7. **Minimal changes** — when updating or refactoring, change only what is necessary. Do not refactor unrelated code.