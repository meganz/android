# CLAUDE.md

This file provides guidance to Claude Code when working in the `:feature:home:home` module.

> Module path: `:feature:home:home` · Build file: `feature/home/home/home.gradle.kts` · Namespace: `mega.privacy.android.feature.home`

## Overview
The Home feature module implements the app's Home screen and its supporting screens. The Home screen is a configurable, widget-based dashboard composed of pluggable widgets (Recents, Chips, Banner, Continue Where Left Off, Viewed Links) plus standalone screens for Recents listing/buckets, "Continue where left off", Viewed Links, "What's New", and Home configuration. It also handles the offline variant of Home.

This is a Compose-only, presentation-layer feature module. It consumes business logic via use cases from `:domain` (it does not define its own use cases or repositories) and integrates with the app shell through the navigation-contract abstractions (`FeatureDestination`, `MainNavItem`, `NavigationHandler`, `TransferHandler`).

## Architecture & Layout
Note: the source package root is `mega.privacy.mobile.home` (differs from the Gradle `namespace`). Sources live under `src/main/java/mega/privacy/mobile/home/`:

- `di/` — Hilt modules (`HomeModule`, `WhatsNewModule`).
- `navigation/` — feature entry points (`HomeFeatureGraph`, `HomeNavItem`).
- `presentation/` — all screens, ViewModels, widgets, models, and mappers, organized by sub-feature:
  - `home/` — main Home screen + offline screen; `widget/` (chips, banner, recents, continuewhereleftoff, viewedlinks), `model/`, `actions/`.
  - `configuration/` — Home widget configuration screen (+ `mapper/`, `model/`).
  - `recents/` — recents list and `bucket/` detail (+ `mapper/`, `model/`, `view/`).
  - `continuewhereleftoff/` — full "continue where left off" list screen.
  - `whatsnew/` — "What's New" screen + versioned `detail/` definitions.

## Key Components
- **ViewModels**: `HomeViewModel`, `HomeConfigurationViewModel`, `WhatsNewViewModel`, `ContinueWhereLeftOffListViewModel`, `RecentsViewModel`, `RecentsBucketViewModel`, `ContinueWhereLeftOffViewModel` (widget), `ViewedLinksViewModel`, `BannerWidgetViewModel`.
- **Use Cases**: None defined here. Use cases are injected from `:domain` (e.g. `GetRecentActionsUseCase`, `MonitorContinueWhereLeftOffItemsUseCase`, `GetPromoBannersUseCase`, `DismissBannerUseCase`, `HasOfflineFilesUseCase`, `MonitorConnectivityUseCase`, `GetFeatureFlagValueUseCase`).
- **Repositories / Gateways / Data sources**: None. This module is presentation-only.
- **Navigation**: `HomeFeatureGraph` (implements `FeatureDestination`, registers child screens on the Navigation3 `EntryProviderScope`); `HomeNavItem` (implements `MainNavItem`, supplies the start destination). Each screen has a `*ScreenDestination.kt` declaring its `@Serializable` `NavKey` and an `EntryProviderScope` extension (e.g. `HomeScreenDestination`, `RecentsScreenDestination`, `RecentsBucketScreenDestination`, `ContinueWhereLeftOffScreenDestination`, `ViewedLinksScreenDestination`, `HomeConfigurationScreenDestination`).
- **UI**: Compose screens — `HomeScreen`, `HomeOfflineScreen`, `HomeConfigurationScreen`, `RecentsScreen`, `RecentsBucketScreen`, `ContinueWhereLeftOffListScreen`, `ViewedLinksScreen`. Home widgets implement the `HomeWidget` interface: `RecentsWidget`, `HomeChipsWidget`, `BannerWidget`, `ContinueWhereLeftOffWidget`, `ViewedLinksWidget` (all bound via `HomeModule`). Mappers: `RecentsUiItemMapper`, `RecentsParentFolderNameMapper`, `WidgetConfigurationItemMapper`, `ContinueWhereLeftOffIconMapper`, `ViewedLinksSortMapper`.

## Module Dependencies
Module deps: `:navigation`, `:core:navigation-contract`, `:core:coroutine`, `:domain`, `:resources:icon-pack`, `:resources:string-resources`, `:core:feature-flags`, `:core:formatter`, `:core:ui-components:node-components`, `:core:ui-components:shared-components`, `:feature:transfers:transfers-snowflake-components`, `:core:transfers`, `:core:analytics:analytics-tracker`, `:shared:nodes`, `:shared:transfers`.

Notable external libs: Compose BOM, `androidx.hilt.navigation`, `material3.adaptive.navigation.suite`, `androidx.navigation3.runtime` (Navigation3), Paging + Paging Compose, `compose.state.events`, Kotlin serialization, MEGA analytics, MEGA core-ui, Timber.

Plugins: `mega.android.library`, `mega.android.library.compose`, `mega.android.hilt`, Kotlin serialization.

## Testing
JUnit 5 + Mockito + Turbine + Truth, per the root `.claude/CLAUDE.md` conventions. Test deps include `:core-test`, `:core-ui-test`, `:core:analytics:analytics-test`, and `androidx.paging.testing` (for Paging-backed ViewModels). Run:

```
./gradlew feature:home:home:testDebugUnitTest
```

## Notes & Gotchas
- Package root (`mega.privacy.mobile.home`) does NOT match the Gradle namespace (`mega.privacy.android.feature.home`) — follow the existing `mega.privacy.mobile.home` package when adding files.
- Navigation uses Navigation3 (`EntryProviderScope<NavKey>`); new screens need a `@Serializable` `NavKey`, a `*ScreenDestination` extension, and registration inside `HomeFeatureGraph`.
- New Home widgets must implement `HomeWidget` and be bound in `di/HomeModule` to appear on the dashboard.
- `WhatsNew` detail content is versioned (e.g. `V16_1_WhatsNewDetail`) and provided through `di/WhatsNewModule` — add a new versioned detail object when shipping new feature highlights.
- Presentation-only module: add business logic as use cases in `:domain`, not here.
- Lint: `CoroutineCreationDuringComposition` is disabled and `abortOnError = true`; unit tests run against `targetSdk = 34`.
