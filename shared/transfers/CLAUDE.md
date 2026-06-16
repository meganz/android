# CLAUDE.md

This file provides guidance to Claude Code when working in the `:shared:transfers` module.

> Module path: `:shared:transfers` · Build file: `shared/transfers/transfers.gradle.kts` · Namespace: `mega.privacy.android.shared.transfers`

## Overview
Small shared module providing reusable Compose UI and state for initiating file uploads from URIs. Its `UploadingFiles` composable wires a `UploadFileViewModel` to caller-provided callbacks/launchers so any feature can prepare picked URIs, resolve name collisions, enforce storage paywall, and emit a `TransferTriggerEvent` for the host's upstream transfer handler (regular folder uploads or chat attachments).

This differs from the sibling transfers modules:
- `:core:transfers` (`mega.privacy.android.core.transfers`) — low-level reusable widgets/extensions such as the transfers toolbar widget and in-progress transfer helpers.
- `:feature:transfers:transfers` (`mega.privacy.android.feature.transfers`) — the full transfers feature, including navigation destinations, DI, and the transfers settings screen.
- `:shared:transfers` (this module) — a thin shared helper focused only on the "pick URIs → start upload" flow, intended to be embedded by multiple features.

## Architecture & Layout
Source root: `src/main/java/mega/privacy/android/shared/`

- `transfers/components/` — Compose UI entry point and the URI event state holder.
- `transfers/model/` — ViewModel and its UI state.

## Key Components
- `UploadingFiles` (`components/UploadingFiles.kt`) — composable that observes the ViewModel's `uiState` and the incoming `urisEvent`, dispatching name-collision, over-quota, start-upload, and error events to the host. Routes URIs to a folder upload or a chat attachment depending on whether `chatIds` is provided.
- `UploadUrisEventState` / `rememberUploadUrisEventState()` (`components/UploadingFiles.kt`) — `@Stable` holder wrapping a `StateEventWithContent<List<Uri>>` to trigger/consume the URI upload event.
- `UploadFileViewModel` (`model/UploadFileViewModel.kt`) — `@HiltViewModel` that prepares URIs (`FilePrepareUseCase`), checks name collisions (`CheckFileNameCollisionsUseCase`), resolves the destination (`GetRootNodeUseCase`), guards against paywall (`MonitorStorageStateEventUseCase`), and emits `TransferTriggerEvent.StartUpload.Files` / `StartChatUpload.Files`.
- `UploadFileUiState` (`model/UploadFileUiState.kt`) — data class of `compose-state-events` for over-quota, name-collision, start-upload, and upload-error events.

## Module Dependencies
Project modules: `:domain`, `:data`, `:navigation`, `:core:feature-flags`, `:core:formatter`, `:core:analytics:analytics-tracker`, `:resources:string-resources`, `:resources:icon-pack`.

Notable external libs: MEGA core-ui + tokens, Compose BOM / Material3, Navigation3 runtime, `compose-state-events`, Hilt navigation, MEGA analytics, Coil3, Timber, Gson, DataStore preferences, Accompanist permissions. Plugins: `mega.android.library`, `mega.android.library.compose`, `mega.android.hilt`, `kotlin-parcelize`. Also consumes the pre-built MEGA SDK (`preBuiltSdkDependency`).

## Testing
JUnit5 + Mockito + Truth (Turbine for Flow). Run: `./gradlew shared:transfers:testDebugUnitTest`

Current coverage: `UploadFileViewModelTest` (`src/test/java/mega/privacy/android/shared/model/UploadFileViewModelTest.kt`).

## Notes & Gotchas
- Note the package/directory mismatch: files live under `.../shared/` directories on disk but declare packages `mega.privacy.android.shared.transfers.*` (and the test under `mega.privacy.android.shared.model`). Match existing package declarations rather than the folder name.
- Every public flow first short-circuits through the paywall check (`StorageState.PayWall` → `overQuotaEvent`); preserve this guard when adding new upload paths.
- All UI signals are one-shot `compose-state-events`; the host is responsible for consuming them (the composable already wires the `onConsume*` callbacks).
- `UploadingFiles` is purely event-dispatching — it renders no visible UI itself; the host supplies the snackbar host (`LocalSnackBarHostState`), name-collision `ActivityResultLauncher`, and `onStartUpload` handler.
