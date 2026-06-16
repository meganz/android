# CLAUDE.md

This file provides guidance to Claude Code when working in the `:core:coroutine` module.

> Module path: `:core:coroutine` · Build file: `core/coroutine/coroutine.gradle.kts` · Namespace: `mega.privacy.android.core.coroutine`

## Overview
`:core:coroutine` is a small, dependency-light core module providing shared Kotlin coroutine and `Flow` extension functions used across the codebase. It centralizes common reactive patterns (UI state sharing, inclusive take-while, exception logging, debug flow logging) so feature and presentation layers can reuse them instead of reimplementing the same boilerplate.

The module deliberately has no Android or DI dependencies beyond `kotlinx-coroutines-core` and Timber for logging; everything is exposed as top-level extension functions.

## Architecture & Layout
- `src/main/java/mega/privacy/android/core/coroutine/` — single package containing all extensions:
  - `FlowExtensions.kt` — general `Flow` operators.
  - `ExceptionLogging.kt` — failure-logging helpers for `Flow` and `Result`.
  - `LogFlow.kt` — debug-only per-emission logging.

## Key Components
- `Flow<T>.asUiStateFlow(scope, initialValue)` — converts a `Flow` into a `StateFlow` using `SharingStarted.WhileSubscribed(5000)` for UI state. This is the standard helper referenced by the project's ViewModel state conventions.
- `Flow<T>.takeWhileInclusive(predicate)` — like `takeWhile` but also emits the first item that fails the predicate.
- `Flow<T>.logAndSwallowExceptions()` — logs upstream exceptions via Timber and swallows them; rethrows `CancellationException`. Drop-in replacement for `catch { Timber.e(it) }`.
- `Result<T>.logAndSwallowExceptions()` — `inline` helper that logs a failed `Result` via Timber and returns it unchanged; rethrows `CancellationException`. Drop-in replacement for `onFailure { Timber.e(it) }`.
- `Flow<T>.logFlow(name, transform)` — logs each emission via Timber, active only when `BuildConfig.DEBUG`; returns the original flow unchanged in release builds.

## Module Dependencies
- `lib.coroutines.core` (kotlinx-coroutines-core)
- `lib.logging.timber`
- `:core-test` (test only)

Note: `buildConfig = true` is enabled so `BuildConfig.DEBUG` is available to `logFlow`.

## Testing
- JUnit 5 with the project's standard unit-test bundles.
- Run: `./gradlew core:coroutine:testDebugUnitTest`

## Notes & Gotchas
- This module provides `Flow`/coroutine helpers; it does NOT define dispatcher qualifiers (`@IoDispatcher`, etc.) — those live elsewhere.
- Both `logAndSwallowExceptions` variants intentionally rethrow `CancellationException`; preserve this when editing so coroutine cancellation keeps propagating.
- `logFlow` is debug-only by design — do not rely on its logging in release builds.
- `asUiStateFlow` hardcodes a 5-second `WhileSubscribed` timeout; this is the shared convention for UI state flows.
- See root `.claude/CLAUDE.md` for global project rules (4-space indentation, naming, testing conventions).
