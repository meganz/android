# CLAUDE.md

This file provides guidance to Claude Code when working in the `:shared:search` module.

> Module path: `:shared:search` · Build file: `shared/search/search.gradle.kts` · Namespace: `mega.privacy.android.shared.search`

## Overview

`:shared:search` is a UI-only, presentation-layer library that provides a reusable search "shell" shared across feature consumers (nodes, chat, contacts, etc.). It owns all the chrome common to any search screen — the search top app bar, an optional filter chips row with its bottom sheet, recent searches, and the landing/loading/empty/results state machine.

The module is deliberately domain-agnostic: it carries no node/chat/contact specific types. Each consumer maps its own UI state into the generic `SearchShellState` and plugs its result rendering in through a `resultsContent` slot. There are no use cases, repositories, ViewModels, or data layer here — just Compose components and immutable state models.

## Architecture & Layout

```
src/main/java/mega/privacy/android/shared/search/
└── presentation/
    ├── SearchShellScaffold.kt   # The top-level reusable scaffold composable
    ├── component/               # Stateless Compose UI building blocks
    └── model/                   # @Immutable UI state / config data classes
```

- `presentation/` — the only package; entry point is `SearchShellScaffold`.
- `presentation/component/` — individual UI pieces composed by the scaffold.
- `presentation/model/` — immutable data classes describing shell state and filter/empty-state config. Text is expressed via `LocalizedText` (core-ui), not raw strings.

## Key Components

**Scaffold (entry point)**
- `SearchShellScaffold` — the reusable shell. Takes a `SearchShellState`, landing/empty content, search/back/recent-search callbacks, optional filter callbacks, and slots: `topBarOverride` (e.g. selection-mode app bar), `bottomBar`, `loadingContent`, and the required `resultsContent`.

**Components** (`presentation/component/`)
- `SearchTopAppBar` — search field + back navigation.
- `SearchFilterChips` — horizontal row of filter chips.
- `SearchFilterBottomSheet` (`SearchFilterBottomSheetContent`) — option picker shown when a chip is tapped.
- `RecentSearchesView` — recent queries list with clear-all.
- `SearchEmptyStateView` — renders the landing and empty states.

**Models** (`presentation/model/`)
- `SearchShellState` — generic, `@Immutable` shell UI state (search text, pre-search/loading/empty flags, recent searches, filters).
- `SearchEmptyContent` — title/description/image config for landing and empty states.
- `SearchFilterChipState` — single chip (id, label, isSelected).
- `SearchFilterOptions` / `SearchFilterOption` — options shown in the filter bottom sheet.

## Module Dependencies

Project modules:
- `:resources:string-resources`, `:resources:icon-pack`
- `:lint` (lint checks), plus the pre-built SDK dependency
- Test only: `:core-test`, `:core-ui-test`

Notable external libraries:
- `lib.mega.core.ui` and `lib.mega.core.ui.tokens` — MEGA core-ui components (`MegaScaffoldWithTopAppBarScrollBehavior`, `MegaModalBottomSheet`, modifiers) and `LocalizedText`.
- AndroidX Compose (BOM), Material 3, Lifecycle ViewModel / runtime-compose.
- Kotlin KTX, Timber.

Convention plugins: `mega.android.library`, `mega.android.library.compose`, `mega.android.hilt`.

## Testing

Tests use JUnit5 + Mockito + Turbine + Truth, with Compose UI test bundles (`core-ui-test`). Existing tests are Compose component/scaffold tests (e.g. `SearchShellScaffoldTest`, `SearchFilterChipsTest`, `SearchFilterBottomSheetContentTest`) driven via `testTag`s.

Run: `./gradlew shared:search:testDebugUnitTest`

## Notes & Gotchas

- Keep this module domain-free. Do not introduce node/chat/contact types, use cases, repositories, or ViewModels — consumers own that and map into `SearchShellState`.
- All user-facing text flows through `LocalizedText`; do not hardcode strings or pass raw `String` labels into models.
- Components are stateless; the scaffold owns transient UI state internally (selected filter id, focus/keyboard, bottom sheet). Selection-mode UI is injected via `topBarOverride` / `bottomBar`, not built in.
- State precedence in the scaffold: `isPreSearch` → `isLoading` → `isEmpty` → results. Pre-search shows recent searches, or the landing content only once `isRecentSearchesLoading` is false.
- `unitTests.targetSdk = 34` is pinned in the build file; keep it when adding Robolectric/Compose tests.
- Public composables and models carry KDoc — keep it accurate when changing parameters/slots.
