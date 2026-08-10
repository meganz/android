# CLAUDE.md

This file provides guidance to Claude Code when working in the `:shared:original-core-ui` module.

> Module path: `:shared:original-core-ui` · Build file: `shared/original-core-ui/original-core-ui.gradle.kts` · Namespace: `mega.privacy.android.core`

## Overview
This is MEGA's "original" Compose design-system / core-ui component library. It provides the shared theme, design tokens, and a large catalog of reusable Compose controls (buttons, text fields, dialogs, lists, chips, app bars, sheets, etc.) consumed by feature and app modules across the project.

The module wraps and extends the external `lib.mega.core.ui` design system (re-exported via `api`) plus its token library (`lib.mega.core.ui.tokens`), adapting them into MEGA-branded composables and applying the app's theming. New UI should be built from these components and tokens rather than from raw Material widgets or hardcoded values.

## Architecture & Layout
All Kotlin lives under `src/main/java/mega/privacy/android/shared/original/core/ui/`:
- `theme/` — theming entry point and tokens: `Theme.kt` (`OriginalTheme`), `Type.kt` (`Typography`, fonts), `Colour.kt`, `LegacyPalette.kt`, `shape/`, and `extensions/` (Colour/Typography/Modifier extensions).
- `controls/` — the component catalog, grouped by family (see below).
- `model/` — UI state/value models (e.g. `DragDropListState`, `KeyboardState`, `ListGridState`, `MegaSpanStyle`, `SpanIndicator`).
- `navigation/` — Compose bottom-sheet navigation helpers (`ExtendedBottomSheetNavigator`, `FolderPicker`).
- `preview/` — `@PreviewParameterProvider` helpers and shared `Previews` annotations.
- `utils/` — assorted Compose utilities.

## Key Components
- **Theme & tokens**: `OriginalTheme` (top-level theme wrapper), `Typography`/`Type.kt`, `Colour.kt`, `LegacyPalette.kt`, shape definitions, and Colour/Typography extension accessors.
- **Text**: `MegaText`, `MegaSpannedText`, `HighlightedText`, `MarqueeText`, `AutoSizeText`, `MiddleEllipsisText`.
- **Buttons**: `RaisedMegaButton`, `OutlinedMegaButton`, `TextMegaButton`, `LinkButton`, `MegaButtonWithIcon`, `MegaFloatingActionButton`, `MegaCheckBox`, `ToggleMegaButton`.
- **Text fields**: `GenericTextField`, `PasswordTextField`, `LabelTextField`, `GenericDescriptionTextField`, plus `TextFieldColors` and input `transformations/`.
- **Dialogs & sheets**: `MegaAlertDialog`, `ConfirmationDialog` (+ variants), `FullScreenDialog`, `ProgressDialog`; `BottomSheet`, `MegaBottomSheetLayout`, `MegaBottomSheetContainer`.
- **Lists**: `GenericTwoLineListItem`, `NodeListViewItem`, `NodeGridViewItem`, `MenuActionListTile`, `DragDropListView`, `BulletListView`.
- **Chips & misc**: `MegaChip`, `HighlightChip`, `ChipBar`, `Badge`; app bars (`MegaAppBar`, `SelectModeAppBar`); plus families for `banners`, `cards`, `tab`, `progressindicator`, `snackbars`, `tooltips`, `menus`, `images`, `pager`, and feature-specific groups (`chat`, `meetings`, `video`, `camera`).

## Module Dependencies
- Project modules: `:core:passcode:passcode`, `:resources:icon-pack`, `:resources:string-resources`; `:core-ui-test` and `:lint` (test/lint only).
- External: `lib.mega.core.ui` (re-exported via `api`), `lib.mega.core.ui.tokens`, the Compose BOM bundle, Material (`google.material`), ConstraintLayout-Compose, Accompanist (systemui/permissions/placeholder), Coil, Balloon, emoji picker, `lib.compose.state.events`, and `kotlinx.collections.immutable`.
- Convention plugins: `mega.android.library`, `mega.android.library.compose`, `mega.android.hilt`.

## Testing
JUnit 5 with Compose UI testing utilities (`testlib.bundles.ui.test`, `testlib.bundles.junit5.api`) and `:core-ui-test`.

Run: `./gradlew shared:original-core-ui:testDebugUnitTest`

## Notes & Gotchas
- This is the "original" / legacy design system. A newer external design system exists in `lib.mega.core.ui`; this module re-exports and adapts it. Prefer existing components and tokens here over hardcoding colors, typography, or shapes, and over raw Material widgets.
- Wrap UI under `OriginalTheme` so colors/typography resolve correctly (light/dark via `drawable-night` and palette).
- The lint rule `CoroutineCreationDuringComposition` is disabled for this module, but `abortOnError = true` — other lint errors fail the build.
- `buildConfig = true` is enabled; the namespace is `mega.privacy.android.core` (not `...shared.original.core.ui`).
