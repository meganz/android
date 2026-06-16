# CLAUDE.md

This file provides guidance to Claude Code when working in the `:legacy-core-ui` module.

> Module path: `:legacy-core-ui` · Build file: `legacy-core-ui/legacy-core-ui.gradle.kts` · Namespace: `mega.privacy.android.legacy.core.ui`

## Overview
`:legacy-core-ui` is a shared library of older, app-wide Jetpack Compose UI components and helpers. Despite the "legacy" name these are Compose composables (built on Material 2 `androidx.compose.material`), not XML/View-based widgets. They predate, and are gradually being superseded by, the newer `:shared:original-core-ui` design system, which this module depends on for theming, colors, models (e.g. `SpanIndicator`, `KeyboardState`) and theme extensions.

Use this module to find existing reusable controls (app bars, dialogs, chips, list items, text fields, etc.) that feature modules consume. New UI work should prefer components from `:shared:original-core-ui` (the current design system) and only fall back here when an equivalent does not yet exist there.

## Architecture & Layout
Single source root `src/main/java/mega/privacy/android/legacy/core/ui/`:
- `controls/` — Compose UI components, grouped by type: `appbar/`, `chips/`, `dialogs/`, `lists/`, `text/`, `textfields/`, `tooltips/`, `controlssliders/`, `divider/`, `keyboard/` (plus `LegacyMegaEmptyView`).
- `model/` — small UI state/model types (`SearchWidgetState`, `SpanStyleWithAnnotation`).
- `TextFieldColors.kt` — top-level shared color helpers.

## Key Components
- App bars: `LegacyTopAppBar`, `SimpleTopAppBar`, `SimpleTopAppBarWithSubtitle`, `SimpleNoTitleTopAppBar`, `LegacySearchAppBar`.
- Dialogs: `MegaDialog`, `InputDialog`, `LoadingDialog`, `EditOccurrenceDialog`.
- Chips: `TextButtonChip`, `TextButtonWithIconChip`, `DropdownMenuChip`, `TextFieldChip`, `PhotoChip`, `CallTextButtonChip`.
- List items: `NodeListViewItem`, `HeaderViewItem`, `MenuActionHeader`, `ImageIconItem`, `MediaQueueItemView`, `LoadingItem`.
- Text: `MegaSpannedText` (renders `[A]…[/A]`-style MEGA tag markup), `MarqueeText`, `MiddleEllipsisText`.
- Other: `MegaTextField`, `LabelledSwitch`, `CustomDivider`, `LegacyMegaTooltip`, `LegacyMegaEmptyView`, `keyboardAsState()` (keyboard visibility as Compose `State`).

## Module Dependencies
- Project: `:resources:icon-pack`, `:shared:original-core-ui`; `:lint` (lintChecks); `:core-ui-test` (test).
- External: Compose BOM + Material, `constraintlayout-compose`, AppCompat, Google Material, Accompanist (systemui / permissions / placeholder), `compose-state-events`, Coil Compose, Balloon (tooltips). Debug-only: KotlinPoet, Gson.

## Testing
JUnit 5 with Compose UI tests (Robolectric — see `src/test/resources/robolectric.properties`). Tests live under `src/test/...` mirroring component packages (e.g. `controls/lists/`, `controls/chips/`, `controls/dialogs/`).

Run: `./gradlew legacy-core-ui:testDebugUnitTest`

## Notes & Gotchas
- Prefer `:shared:original-core-ui` design-system components for new code; treat additions here as maintenance of existing usages rather than expansion.
- Components use Material 2 (`androidx.compose.material`), not Material 3 — match the surrounding API when editing.
- Theming, colors and shared models come from `:shared:original-core-ui` (e.g. `theme.extensions`, `SpanIndicator`, `KeyboardState`); reuse those rather than redefining them here.
- `lint { abortOnError = true }` — lint failures break the build.
