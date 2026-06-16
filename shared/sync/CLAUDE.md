# CLAUDE.md

This file provides guidance to Claude Code when working in the `:shared:sync` module.

> Module path: `:shared:sync` · Build file: `shared/sync/sync.gradle.kts` · Namespace: `mega.privacy.android.shared.sync`

## Overview
Small shared module holding sync-related UI helpers, mappers, and feature flags that are reused across modules (e.g. Device Center and the full sync feature) without pulling in the heavyweight `:feature:sync` module.

This is distinct from `:feature:sync` (namespace `mega.privacy.android.feature.sync`, ~230 source files), which contains the complete Sync/Backup feature with its own data, domain, and presentation layers. `:shared:sync` is intentionally tiny (3 source files) and exposes only common, low-level pieces that several callers need.

## Architecture & Layout
- `mega.privacy.android.shared.sync` — root: shared mappers (`DeviceFolderUINodeErrorMessageMapper`).
- `mega.privacy.android.shared.sync.ui` — shared Compose components (`SyncEmptyState`).
- `mega.privacy.android.shared.sync.featuretoggles` — sync feature flags (`SyncFeatures`).

## Key Components
- **DeviceFolderUINodeErrorMessageMapper** — `@Inject` mapper with `operator fun invoke(SyncError?): Int?` mapping a domain `SyncError` to a `@StringRes` error message id (or null).
- **SyncEmptyState** (in `DeviceCenterEmptyState.kt`) — `@Composable` rendering a centered icon + text empty state, using `MegaText` from `:shared:original-core-ui`.
- **SyncFeatures** — `Feature` enum of sync feature flags (`SyncFrequencySettings`, `DisableBatteryOptimization`) with a `FeatureFlagValueProvider` companion supplying default values. Register new flags at the top of the list.

## Module Dependencies
- `:domain` — `SyncError`, `Feature`, `FeatureFlagValueProvider` entities.
- `:resources:string-resources` — `R.string.*` sync error messages.
- `:shared:original-core-ui` — `MegaText` and theme values.
- `:lint` (lint checks), Compose BOM bundle.

## Testing
JUnit5 + Mockito + Truth. Run: `./gradlew shared:sync:testDebugUnitTest`

## Notes & Gotchas
- The composable is named `SyncEmptyState` but lives in `DeviceCenterEmptyState.kt` — search by symbol, not file name.
- Do not confuse with `:feature:sync`; keep this module small and dependency-light. Add new code here only when it must be shared by multiple sync consumers.
- New `SyncFeatures` entries should be added at the top of the enum to minimize git diffs.
