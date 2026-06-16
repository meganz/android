# CLAUDE.md

This file provides guidance to Claude Code when working in the `:domain` module.

> Module path: `:domain` · Build file: `domain/domain.gradle.kts` · Namespace: n/a (pure Kotlin JVM library — no Android namespace)

## Overview
`:domain` is the innermost layer of the project's clean architecture. It holds the business logic of the entire app: use cases, repository interfaces, domain entities, and domain-specific exceptions. It is a pure Kotlin/JVM library (uses the `mega.jvm.library` convention plugin, not the Android library plugin) and intentionally has no Android framework dependencies.

It is the dependency hub of the codebase: `:data` implements the repository interfaces declared here, and effectively every feature module depends on `:domain` for use cases and entities. It is very large (~3400 Kotlin files), so explore by package rather than reading exhaustively.

## Architecture & Layout
Source root: `src/main/kotlin/mega/privacy/android/domain/`

- `usecase/` — the bulk of the module; ~81 feature-scoped subpackages (e.g. `account`, `auth`, `chat`, `call`, `login`, `camerauploads`, `node`, `photos`, `notifications`, `billing`, `meeting`). `usecase/impl/` holds implementations where an interface/impl split is used.
- `repository/` — ~59 repository interfaces (e.g. `AccountRepository`, `ChatRepository`, `CameraUploadsRepository`, `VideoRepository`), implemented in `:data`.
- `entity/` — domain models, organized by feature subpackage (e.g. `account`, `chat`, `call`, `node`, `photos`, `meeting`, `billing`).
- `exception/` — ~50 domain exceptions (e.g. `NotEnoughQuotaMegaException`, `ChatRoomDoesNotExistException`, `BusinessAccountUnverifiedException`).
- `di/` — Hilt modules wiring use case interface/impl bindings (e.g. `InternalSharedUseCaseModule`, `BillingModule`).
- `qualifier/`, `extension/`, `featuretoggle/`, `monitoring/`, `logging/` — supporting cross-cutting helpers and qualifiers.

## Key Components
- **Use Cases**: Single-responsibility classes with `@Inject` constructor and an `operator fun invoke(...)`, depending on repository interfaces. Examples: `LoginUseCase`, `SaveAccountCredentialsUseCase`, `MonitorEphemeralCredentialsUseCase`.
- **Repository interfaces**: Declared here, implemented as `Default*Repository` in `:data`. Examples: `AccountRepository`, `ChatRepository`, `CameraUploadsRepository`.
- **Entities**: Plain domain models, often data/sealed classes and enums. Examples: `AccountBlockedType`, `AccountStorageDetail`, `AccountPlanDetail`.

## Module Dependencies
From `domain.gradle.kts`:
- Plugins: `mega.jvm.library`, `mega.jvm.hilt`, Kotlin serialization.
- `lib.coroutines.core`, `lib.javax.inject`, `lib.kotlin.serialisation`, `androidx.paging.core`.
- `lintChecks(project(":lint"))`; lint runs with `abortOnError` and `warningsAsErrors`.

Keep this module Android-free. It depends on no other project module (except `:lint` for checks). The only "Android" coupling is `androidx.paging.core`, which is JVM-safe.

## Testing
JUnit 5 + Mockito + Turbine + Truth via the `unit.test` / `junit5` test bundles. Run with:

```
./gradlew domain:test
```

## Notes & Gotchas
- Dependency direction: nothing flows inward. `:data` and all feature modules depend on `:domain`; `:domain` depends on none of them.
- Do not introduce Android SDK dependencies here — that would break the layer boundary and the JVM library plugin.
- Very large module — use cases and entities are organized by feature area; navigate by subpackage and use `grep`/`find`, do not read whole trees.
- Global conventions (use case / repository / naming patterns) live in the root `.claude/CLAUDE.md` and the `usecase` skill; follow those.
