# CLAUDE.md

This file provides guidance to Claude Code when working in the `:core:transfers` module.

> Module path: `:core:transfers` · Build file: `core/transfers/transfers.gradle.kts` · Namespace: `mega.privacy.android.core.transfers`

## Overview
`:core:transfers` is a small, shared presentation-layer module providing reusable transfer UI building blocks for the app. Its primary export is the `TransfersToolbarWidget` Composable, which renders the live transfers progress indicator shown in toolbars, and a set of Compose extension helpers for formatting in-progress transfer info (speed, progress size, percentage).

It is consumed by feature/app screens that need to surface ongoing transfer status. It depends on `:domain` for transfer monitoring use cases and on `:feature:transfers:transfers-snowflake-components` for the actual animated widget rendering, acting as the wiring layer between domain state and the shared visual component.

## Architecture & Layout
- `widget/` — the toolbar widget: stateless Composable, its `@HiltViewModel`, and the UI state data class.
- `extension/` — Compose extension functions on domain transfer entities for display formatting.

## Key Components
- **`TransfersToolbarWidget`** (`widget/TransfersToolbarWidget.kt`) — `@Composable` entry point. Collects state from the ViewModel and delegates rendering to `TransfersToolbarWidgetViewAnimated` (from `:feature:transfers:transfers-snowflake-components`). Tracks an analytics event and invokes `onClick` only when the user is logged in.
- **`TransfersToolbarWidgetViewModel`** (`widget/TransfersToolbarWidgetViewModel.kt`) — `@HiltViewModel`. Combines `MonitorTransfersStatusUseCase`, `MonitorConnectivityUseCase`, `MonitorLastTransfersHaveBeenCancelledUseCase`, `MonitorTransferInErrorStatusUseCase`, and `MonitorUserCredentialsUseCase` to derive a `TransfersToolbarWidgetStatus` (Transferring, Paused, OverQuota, Error, Completed, Idle). Samples status emissions (default 500ms) and debounces offline state.
- **`TransfersToolabarWidgetUiState`** (`widget/TransfersToolabarWidgetUiState.kt`) — UI state data class (note the existing `Toolabar` spelling in the filename/class). Holds status, transferred/total sizes, and flags for cancellation, error, and login state.
- **`InProgressTransferExt`** (`extension/InProgressTransferExt.kt`) — Compose extension functions on `InProgressTransfer`: `getSpeedString`, `getProgressSizeString`, `getProgressPercentString`, with byte-unit (KB…EB) formatting and over-quota/paused/queued string handling.

## Module Dependencies
- `:domain` — transfer/connectivity/credentials monitoring use cases and transfer entities.
- `:feature:transfers:transfers-snowflake-components` — provides `TransfersToolbarWidgetViewAnimated` and `TransfersToolbarWidgetStatus`.
- `:core:formatter` — file-size formatting (`formatFileSize`).
- `:core:navigation-contract`, `:navigation` — navigation contracts.
- `:core:analytics:analytics-tracker`, `lib.mega.analytics` — analytics event tracking.
- `:resources:icon-pack`, `:resources:string-resources` — shared resources.
- `:core:ui-components:node-components`, `lib.mega.core.ui` — shared Compose UI.
- External: Jetpack Compose (BOM), Hilt navigation, Lifecycle Compose, Navigation3 runtime, kotlinx-serialization, Timber.

## Testing
JUnit5 + Mockito + Truth (+ Turbine for Flow/state). Run: `./gradlew core:transfers:testDebugUnitTest`. Current coverage: `TransfersToolbarWidgetViewModelTest`.

## Notes & Gotchas
- The UI state class name `TransfersToolabarWidgetUiState` contains a pre-existing typo (`Toolabar`); match it exactly when referencing.
- `samplePeriod` is an injected constructor parameter; `0`/negative disables sampling, `null` falls back to `DEFAULT_SAMPLE_PERIOD` (500ms).
- This module renders the widget but does NOT define the animated visuals — those live in `:feature:transfers:transfers-snowflake-components`.
- See root `.claude/CLAUDE.md` for global ViewModel/Compose/testing conventions.
