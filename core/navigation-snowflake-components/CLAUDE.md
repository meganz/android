# CLAUDE.md

This file provides guidance to Claude Code when working in the `:core:navigation-snowflake-components` module.

> Module path: `:core:navigation-snowflake-components` · Build file: `core/navigation-snowflake-components/navigation-snowflake-components.gradle.kts` · Namespace: `mega.privacy.android.navigation.snowflake`

## Overview
A "snowflake" library of reusable Jetpack Compose UI for the app's **main navigation chrome** — the adaptive bottom navigation bar / navigation rail that hosts the top-level destinations. It renders the `MainNavItem`s contributed through `:core:navigation-contract` (icon, label, badge, preferred slot) and reports selection/visibility back to the host via `NavigationUiController`.

It is presentation-only: it owns no ViewModels, use cases, or data layer. Callers pass in an already-resolved set of navigation items plus selection/click callbacks. Layout is adaptive — a `NavigationRail` in landscape and a bottom `NavigationSuite` (bar/rail by window size) in portrait.

## Architecture & Layout
Source root: `src/main/java/mega/privacy/mobile/navigation/snowflake/` (note the `mega.privacy.mobile.*` package root, which differs from the Gradle `namespace`).
- **(root)** — `MainNavigationScaffold` (the entry-point scaffold), `NavigationBadge`, `IndicatorDot`, `NavigationScaffoldColors`.
- `item/` — per-item composables: `MainNavigationIcon`, `MainNavigationItemBadge`.
- `model/` — `NavigationItem` (`@Immutable` item model) and `NavigationAnimationConfig`.

## Key Components
- **`MainNavigationScaffold`** — the top-level composable. Takes an `ImmutableSet<NavigationItem>`, `onDestinationClick(MainNavItemNavKey)`, an `isSelected(NavKey)` predicate, an optional `NavigationAnimationConfig` and per-item icon slot, the `navContent(NavigationUiController)` body, and an `availableSlots` cap (default 5). Picks `NavigationRail` vs. `NavigationSuite` from orientation/window size and orders items by `PreferredSlot`.
- **`NavigationItem`** (`model/`) — `@Immutable` data class: `destination: MainNavItemNavKey`, icons (default + optional selected), `@StringRes label`, `isEnabled`, optional `MainNavItemBadge`, optional analytics `NavigationEventIdentifier`, `PreferredSlot`, and a derived `testTag`. `getIcon(isSelected)` returns the selected icon when present and selected.
- **`NavigationBadge`** — maps the `MainNavItemBadge` variants (`IconBadge` / `NumberBadge` / `TextBadge`, plus the small dot) to core-ui `NotificationBadge`.
- **`IndicatorDot`** / **`NavigationScaffoldColors`** — small visual helpers for the selected-item indicator and scaffold theming.

## Module Dependencies
- Project: `:core:navigation-contract` (the `MainNavItem*`, `NavKey`, `NavigationUiController`, `PreferredSlot` contracts it renders), `:core:analytics:analytics-tracker`, `:resources:icon-pack`, `:resources:string-resources`.
- External: Compose BOM, `androidx.navigation.compose`, `androidx.navigation3.runtime`, Material3 adaptive navigation-suite, `kotlinx.collections.immutable`, MEGA core-ui + tokens, MEGA analytics, Timber.
- Convention plugins: `mega.android.library`, `mega.android.library.compose`, plus `kotlin-parcelize` and the Kotlin serialization plugin.

## Testing
JUnit 5 + Compose UI tests. Tests live under `src/test/kotlin/...` (`MainNavigationScaffoldTest`, `NavigationBadgeTest`). Run:
`./gradlew core:navigation-snowflake-components:testDebugUnitTest`

## Notes & Gotchas
- **Namespace vs. package**: the Gradle `namespace` is `mega.privacy.android.navigation.snowflake`, but sources live under the `mega.privacy.mobile.navigation.snowflake` package — follow the existing `mega.privacy.mobile.*` package when adding files.
- "Snowflake" = a standalone, dependency-light reusable component module; keep it presentation-only (no ViewModels/use cases/data). It renders contracts from `:core:navigation-contract` rather than depending on feature modules.
- Item ordering is driven by `PreferredSlot` and capped at `availableSlots` (default 5) — extra items beyond the cap are not shown; account for this when adding nav items.
- Selected-item analytics are tracked via the `Analytics` singleton inside the scaffold; supply `analyticsEventIdentifier` on `NavigationItem` to enable it.
- Layout adapts to orientation/window size (rail vs. bottom bar) — verify both when changing layout or insets.
- See root `.claude/CLAUDE.md` for global Compose / Material 3 and 4-space formatting conventions.
