# CLAUDE.md

This file provides guidance to Claude Code when working in the `:feature:transfers:transfers` module.

> Module path: `:feature:transfers:transfers` · Build file: `feature/transfers/transfers/transfers.gradle.kts` · Namespace: `mega.privacy.android.feature.transfers`

## Overview
Feature module for the file transfers area. The current source set is focused on the **Transfers Settings** screen, which lets the user configure the maximum number of concurrent download and upload connections. Transfer business logic (use cases, repositories) lives in `:domain`; this module wires the presentation layer and registers its navigation destination through the navigation contract.

Reusable Compose UI for transfer lists, items and the toolbar widget lives in the paired snowflake submodule `:feature:transfers:transfers-snowflake-components` (see below).

## Architecture & Layout
Standard MEGA feature layout under `src/main/java/mega/privacy/android/feature/transfers/`. Only the `presentation` layer is present locally; domain/data are consumed from `:domain`.

- `presentation/settings/` — `TransfersSettingsViewModel`
- `presentation/settings/model/` — `TransfersSettingsUiState`
- `presentation/settings/view/` — `TransfersSettingsView` (Compose)
- `navigation/` — `TransfersFeatureDestination`
- `di/` — `TransfersModule`

## Key Components
- **ViewModels**: `TransfersSettingsViewModel` — exposes a lazy `StateFlow<TransfersSettingsUiState>` built by `combine`-ing the initial max download/upload connections and the valid range with two mutable "new value" flows, materialized via `asUiStateFlow`; provides `setMaxDownloadConnections` / `setMaxUploadConnections`.
- **Use Cases**: Consumed from `:domain` (`mega.privacy.android.domain.usecase.transfers.*`): `GetMaxDownloadConnectionsUseCase`, `GetMaxUploadConnectionsUseCase`, `SetMaxDownloadConnectionsUseCase`, `SetMaxUploadConnectionsUseCase`, `GetMaxTransferConnectionsRangeUseCase`. No use cases defined in this module.
- **Repositories / Gateways / Data sources**: None defined here; all data access is delegated to `:domain` use cases.
- **Navigation**: `TransfersFeatureDestination` implements `FeatureDestination` and registers an `entry<TransfersSettingsNavKey>` (NavKey from `:navigation`) using Navigation3. Provided into the multibinding `Set<FeatureDestination>` by `TransfersModule`.
- **UI**: `TransfersSettingsView` — stateless Compose screen rendering the settings; `TransfersSettingsUiState` is a sealed interface with `Loading` and `Data(maxDownloadConnections, maxUploadConnections, maxTransferConnectionsRange)`.

## Module Dependencies
Project modules: `:domain`, `:navigation`, `:core:navigation-contract`, `:core:coroutine`, `:core:analytics:analytics-tracker`, `:resources:icon-pack`, `:resources:string-resources`, `:lint` (lintChecks). Test: `:core:analytics:analytics-test`, `:core-test`, `:core-ui-test`.

Notable external libs: MEGA core-ui (`lib.mega.core.ui`), MEGA analytics, Jetpack Compose BOM + activity/viewmodel, `androidx.navigation3.runtime`, `androidx.hilt.navigation`, Material3 adaptive navigation suite, Kotlin serialization, compose-state-events, Timber, Hilt. Pulls in the prebuilt MEGA SDK via `preBuiltSdkDependency`.

## Snowflake Components
`:feature:transfers:transfers-snowflake-components` (namespace `mega.privacy.android.feature.transfers.components`) holds the reusable Compose UI building blocks for transfers, depending only on resources + core-ui + Compose/Material3 adaptive + Coil (no `:domain`). Main components under `components/`:
- `ActiveTransferItem`, `CompletedTransferItem`, `FailedTransferItem`, `CameraUploadsTransferItem` — transfer list row items
- `CompletedTransferBottomSheetHeader`, `FailedTransferBottomSheetHeader` — bottom sheet headers
- `SelectedTransferIcon`, `TransferImage` — supporting visuals
- `components/widget/` — `TransfersToolbarWidgetView` and `TransfersToolbarWidgetStatus`

Do NOT add a separate CLAUDE.md for the snowflake submodule.

## Testing
JUnit5 + Mockito + Turbine + Truth per the root conventions. Run:
`./gradlew feature:transfers:transfers:testDebugUnitTest`
(Snowflake: `./gradlew feature:transfers:transfers-snowflake-components:testDebugUnitTest`.)

## Notes & Gotchas
- `uiState` is built lazily and emits `Loading` until all three initial use-case `flow { emit(...) }` sources resolve; each source is individually `catch`-guarded with `Timber.e`, so failures fall back silently rather than surfacing an error state.
- Setters optimistically push the new value into a `MutableStateFlow` only `onSuccess`; on failure the change is logged and the UI keeps the previous value.
- Navigation uses Navigation3 (`androidx.navigation3.runtime`) with `TransfersSettingsNavKey` owned by `:navigation`; back navigation is handled via `navigationHandler.remove(key)`.
- See root `.claude/CLAUDE.md` for global architecture, naming and convention rules.
