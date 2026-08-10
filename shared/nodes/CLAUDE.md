# CLAUDE.md

This file provides guidance to Claude Code when working in the `:shared:nodes` module.

> Module path: `:shared:nodes` · Build file: `shared/nodes/nodes.gradle.kts` · Namespace: `mega.privacy.android.shared.nodes`

## Overview
`:shared:nodes` holds the shared presentation-layer logic and Compose UI for rendering MEGA nodes (files and folders) that is reused across multiple feature modules (e.g. Cloud Drive, shares, search, file pickers). It owns the reusable building blocks for showing node lists/grids, headers, selection mode, sort options, thumbnails, and common node dialogs/bottom sheets, so individual features don't reimplement node display.

It also provides the mappers that turn domain `TypedNode`s into ready-to-render UI models, including icon, subtitle, and thumbnail resolution. It is a UI/presentation library — it depends on `:domain` and `:data` but contains no SDK access of its own.

## Architecture & Layout
Packages under `src/main/java/mega/privacy/android/shared/nodes/`:
- `components/` — Compose UI for node display: `NodesView`, `NodeListView`/`NodeListViewItem`, `NodeGridView`/`NodeGridViewItem`, `NodeViewWithHeader`, `NodeHeaderItem`, `NodeThumbnailView`, `NodeSelectionModeAppBar`, `NodeSkeletons`, `NodeLabelCircle`, `SortBottomSheet`. `components/previewdata/` holds `@Preview` data providers and `NodeUiItemFactory`.
- `mapper/` — domain→UI mappers (see Key Components).
- `model/` — UI state and item models plus `NodeHeaderItemViewModel`/`NodeHeaderItemUiState`.
- `selection/` — multi-selection state holders (`NodeSelectionState`, `SelectableTypedNode`).
- `dialog/` — shared dialogs: new file (`dialog/newfile/`), new folder (`dialog/newfolder/`), `TakeDownDialog`, `DiscardScanWarningDialog`. The new-file/new-folder dialogs include their own ViewModel + UiState.
- `sheet/` — bottom sheets such as `PublicLinkAuthAlertBottomSheet`.
- `extension/` — `NodeIconExt` (node icon resolution helpers).

## Key Components
- **Mappers**: `NodeUiItemMapper` and `NodeViewItemMapper` (convert `TypedNode` → UI item; built for thousands of items), `NodeSubtitleMapper`, `FileTypeIconMapper` (`@Singleton`, extension→icon), `NodeSortConfigurationUiMapper`, `NodeSourceTypeToSearchTargetMapper`.
- **Models**: `NodeViewItem` (current pattern) and `NodeUiItem` (deprecated, migrate to `NodeViewItem`), `TypedNodeItem`, `SelectableNodeItem`, `NodeSubtitleText`, `NodeSortOption`, `SortOptionItem`, `NodeSourceTypeInt`.
- **State/ViewModels**: `NodeHeaderItemViewModel` (Hilt assisted-factory, exposes `StateFlow` via `asUiStateFlow`) with `NodeHeaderItemUiState`; `NodeSelectionState` (`@Stable` rememberSaveable selection holder).
- **UI helpers**: `NodesView` / `NodeListView` / `NodeGridView` composables and `NodeThumbnailView`, used by feature modules to render node collections.

## Module Dependencies
- Project deps: `:domain`, `:data`, `:navigation`, `:core:navigation-contract`, `:core:coroutine`, `:core:feature-flags`, `:core:formatter`, `:core:analytics:analytics-tracker`, `:resources:string-resources`, `:resources:icon-pack`; `:lint` (lintChecks) and the pre-built MEGA SDK.
- External: MEGA core-ui + core-ui-tokens, Compose BOM / Material3 / Navigation3 runtime, Coil3, MEGA analytics, kotlinx-serialization, Gson, DataStore preferences, Hilt navigation, Timber.

## Testing
Unit tests use JUnit 5 + Mockito + Turbine + Truth (test deps include `:core-test`, `:core-ui-test`, `:core:analytics:analytics-test`).
Run: `./gradlew shared:nodes:testDebugUnitTest`

## Notes & Gotchas
- "Shared" here means shared **presentation/UI** across feature modules — not domain logic. Use cases and repository interfaces belong in `:domain`; this module consumes them. Reusable node UI/mappers that more than one feature needs go here rather than in a single `:feature:*` module.
- Prefer `NodeViewItem` for new code; `NodeUiItem` is `@Deprecated` in favor of the new selection-state pattern.
- This module does not touch the MEGA SDK directly — node data arrives as already-mapped `:domain` entities (`TypedNode`).
