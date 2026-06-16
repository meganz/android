# CLAUDE.md

This file provides guidance to Claude Code when working in the `:shared:account` module.

> Module path: `:shared:account` · Build file: `shared/account/account.gradle.kts` · Namespace: `mega.privacy.android.shared.account`

## Overview
`:shared:account` holds account-related presentation logic and Compose UI helpers that are reused across feature modules rather than living in a single feature. It is a presentation-layer Android library (no data layer): ViewModels here consume domain use cases directly and expose state to host screens.

Current scope covers two concerns: surfacing storage/transfer over-quota status (with a reusable banner), and gating UI until an in-progress login/fetch-nodes flow completes.

## Architecture & Layout
Source root: `src/main/java/mega/privacy/android/shared/account/`

- `login/` — `LoginInProgress*` ViewModel, state, and a Compose container used to defer content rendering while login is in progress.
- `overquota/` — over-quota ViewModel plus three sub-packages:
  - `overquota/model/` — UI/state models and enums (`OverQuotaStatus`, `OverQuotaIssue`, `OverQuotaStatusUiState`, `StorageOverQuotaCapacity`).
  - `overquota/mapper/` — Hilt-injected mappers from domain entities (`OverQuotaStatusMapper`, `StorageCapacityMapper`).
  - `overquota/view/` — Compose UI (`OverQuotaBanner`).

## Key Components
- **ViewModels**
  - `OverQuotaStatusViewModel` — combines `MonitorStorageStateUseCase`, `MonitorTransferOverQuotaUseCase`, `MonitorAccountDetailUseCase`, and almost-full-storage banner visibility use cases; exposes `StateFlow<OverQuotaStatusUiState>` and a `dismissWarning()` action.
  - `LoginInProgressViewModel` — polls the `@LoginMutex` and `IsMegaApiLoggedInUseCase` to expose whether login is in progress.
- **Compose UI**
  - `OverQuotaBanner` (`overquota/view/`) — renders warning/error top banners with analytics events.
  - `LoginInProgressContainer` (`login/`) — shows `loadingView` while login is in progress, otherwise `content`.
- **Models**: `OverQuotaStatus`, `OverQuotaIssue` (Storage/Transfer severity), `OverQuotaStatusUiState` (Loading/Data sealed interface), `StorageOverQuotaCapacity`, `LoginInProgressState`.
- **Mappers**: `OverQuotaStatusMapper`, `StorageCapacityMapper` (map `StorageState`/transfer flags to UI models).

## Module Dependencies
- `:domain` — use cases and entities (`StorageState`, account/transfer monitors, `@LoginMutex`).
- `:resources:string-resources`, `:resources:icon-pack` — strings and icons.
- `:core:analytics:analytics-tracker`, `:core:navigation-contract`, `:core:coroutine` (`asUiStateFlow`).
- External: `lib.mega.core.ui` + `core.ui.tokens` (MEGA core-ui Compose components/theme), `lib.mega.analytics`, Compose BOM + Material3, AndroidX Lifecycle / Hilt navigation, Timber.
- Plugins: `mega.android.library`, `mega.android.library.compose`, `mega.android.hilt`.

## Testing
JUnit 5 + Mockito + Turbine + Truth. Tests live in `src/test/...` (`OverQuotaStatusViewModelTest`, `OverQuotaBannerTest`, the latter using Compose UI test rules).

Run: `./gradlew shared:account:testDebugUnitTest`

## Notes & Gotchas
- **Stale duplicate files**: `overquota/OverQuotaStatus.kt`, `overquota/OverQuotaBanner.kt`, `overquota/OverQuotaStatusMapper.kt`, `overquota/StorageCapacityMapper.kt`, `overquota/StorageOverQuotaCapacity.kt`, and `overquota/OverQuotaIssue.kt` (top-level package, no sub-folder) appear to be leftover copies. The production ViewModel and tests import the `overquota.model` / `overquota.mapper` / `overquota.view` versions — edit those, not the top-level ones.
- This module has no data layer; do not add repository implementations here. Inject domain use cases into ViewModels instead.
- `LoginInProgressViewModel` depends on the shared `@LoginMutex` — its `state` flow polls (100 ms) and also short-circuits when `IsMegaApiLoggedInUseCase()` is true to handle the fetch-nodes-still-locked case.
