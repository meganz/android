# CLAUDE.md

This file provides guidance to Claude Code when working in the `:feature:text-editor:text-editor` module.

> Module path: `:feature:text-editor:text-editor` · Build file: `feature/text-editor/text-editor/text-editor.gradle.kts` · Namespace: `mega.privacy.android.feature.texteditor`

## Overview
This module hosts the Jetpack Compose text editor feature. It lets users view, create, and edit plain-text files (regular nodes, file/folder links, chat files, and local paths), with support for line numbers, sharing/exporting links, downloading, sending to chat, and "continue where left off" scroll restoration.

To stay responsive on very large files, content is loaded gradually and rendered in chunks (`CHUNK_SIZE = 1000` lines, capped at `CHUNK_MAX_CHARS = 50_000` chars per chunk) to avoid main-thread ANRs from native text measurement. The module is presentation-only: all data access flows through domain use cases.

## Architecture & Layout
Single `presentation` layer under `mega.privacy.android.feature.texteditor.presentation`. There is no `data`/`domain`/`di`/`navigation` package in this module; business logic lives in `:domain` and the ViewModel is wired with an assisted Hilt factory rather than a navigation destination/NavKey defined here.
- `presentation/` — screen, ViewModel, read-through helper, bottom-bar mapper
- `presentation/model/` — UI state and action/effect models

## Key Components
- **ViewModels**: `TextEditorComposeViewModel` — `@HiltViewModel(assistedFactory = Factory::class)` with `@AssistedInject`. Takes an `@Assisted Args` data class (node handle, `TextEditorMode`, file name, chat/link/local-path params, flags). `MutableStateFlow<TextEditorComposeUiState>`; drives View/Edit/Create modes.
- **Use Cases** (injected from `:domain`): `GetTextContentForTextEditorUseCase`, `GetTextContentForFileLinkUseCase`, `GetTextContentForFolderLinkUseCase`, `SaveTextContentForTextEditorUseCase`, `Get/SetShowLineNumbersPreferenceUseCase`, `GetNodeByIdUseCase`, `GetNodeAccessUseCase`, `ExportNodeUseCase`, `AttachMultipleNodesUseCase`, `Get1On1ChatIdUseCase`, `GetChatFileUseCase`, `GetPublicNodeUseCase`, `MapTypedNodeToPublicLinkUseCase`, `GetPublicChildNodeFromIdUseCase`, `GetNodeVersionsByHandleUseCase`, `MonitorNodeUpdatesUseCase`, `MonitorConnectivityUseCase`, `IsConnectedToInternetUseCase`, `GetFeatureFlagValueUseCase`, and the continue-where-left-off use cases (`Save/Get TextEditorScroll`, `Save/Remove/SaveIfQualifies RecentlyUsedItem`).
- **Repositories / Gateways / Data sources**: none in this module (all data access is via domain use cases).
- **Navigation**: no destination/NavKey defined here. The screen is hosted elsewhere; this module exposes `TextEditorScreen` and the ViewModel `Factory`/`Args`. Uses `SnackbarEventQueue` from `:core:navigation-contract` for snackbars.
- **UI**: `TextEditorScreen` (Compose, `MegaScaffold` + core-ui components). Supporting presentation code: `TextEditorBottomBarActionsMapper` (`@Inject` + `operator fun invoke`), `computeReadThroughFraction` in `TextEditorReadThrough.kt`, and models `TextEditorComposeUiState`, `TextEditorBottomBarAction`, `TextEditorTopBarAction`, `TextEditorNodeEffect`.

## Module Dependencies
Project modules: `:core:navigation-contract`, `:domain`, `:navigation`, `:resources:icon-pack`, `:resources:string-resources`, `:feature:text-editor:text-editor-snowflake-components`.
Notable external libs: Compose BOM (material, material3, icons), `androidx.hilt.navigation`, `androidx.navigation3.runtime`, `lib.mega.core.ui`, `compose-state-events` (one-shot UI events), Kotlin serialization, Timber.
Convention plugins: `mega.android.library`, `mega.android.library.compose`, `mega.android.hilt`, plus Kotlin serialization.

## Snowflake Components
Paired submodule `:feature:text-editor:text-editor-snowflake-components` (namespace `mega.privacy.android.feature.texteditor.components`) holds pure, reusable Compose UI extracted from this feature:
- `TextEditorContent.kt` — the chunked content/gutter rendering composables.
- `TextEditorFastScrollbar.kt` — `TextEditorFastScrollbar` composable plus testable helpers `calculateScrollProportion`, `calculateScrollTarget`, `shouldShowScrollbar` (tested in `TextEditorFastScrollbarTest`).
It depends only on core-ui, resources, and Compose (no `:domain`). Do NOT create a separate CLAUDE.md for it.

## Testing
JUnit 5 + Mockito + Turbine + Truth. Run: `./gradlew feature:text-editor:text-editor:testDebugUnitTest`. Snowflake tests (incl. Compose UI test) run via `./gradlew feature:text-editor:text-editor-snowflake-components:testDebugUnitTest`.

## Notes & Gotchas
- Chunking is correctness-critical: `CHUNK_SIZE`/`CHUNK_MAX_CHARS` exist to prevent ANRs from `MeasuredText.nBuildMeasuredText` on long lines (minified JSON, base64). Don't render whole-file content in one composable.
- `computeReadThroughFraction` derives progress from chunk pixel geometry (View mode chunks by char capacity, not fixed line counts) — keep it pixel/line-span based, not chunk-index based.
- ViewModel is assisted-injected: obtain it via `TextEditorComposeViewModel.Factory.create(args)`, not plain `hiltViewModel()`.
- Mode behavior differs: View vs Edit vs Create change save/exit/discard flows (e.g. `shouldPopDestinationOnCleanEditExit`). Verify all three modes when touching exit/save logic.
- Project-wide conventions live in the root `.claude/CLAUDE.md`.
