# CLAUDE.md

This file provides guidance to Claude Code when working in the `:feature:cloud-drive:cloud-drive` module.

> Module path: `:feature:cloud-drive:cloud-drive` · Build file: `feature/cloud-drive/cloud-drive/cloud-drive.gradle.kts` · Namespace: `mega.privacy.android.feature.clouddrive`

## Overview
Cloud Drive feature module providing the presentation layer for browsing and acting on MEGA nodes: the Cloud Drive browser, Rubbish Bin, Shares (incoming/outgoing/links), Offline files, Favourites, Search, Audio playback, and public Folder/File link handling. It also wires the "Drive/Sync" top-level navigation item.

This is a presentation-only module (Jetpack Compose + Navigation3). It contains no `data` or `domain` source folders — business logic comes from `:domain` and other shared/core modules via use cases injected into the ViewModels.

## Architecture & Layout
All code lives under `presentation/` (one package per screen), plus `di/` and `navigation/`:
- `presentation/{clouddrive, rubbishbin, shares, offline, favourites, search, audio, folderlink, filelink, publiclink, drivesync}/` — each feature area holds its `*ViewModel`, `*Screen`, `*ScreenDestination`, and a `model/` (UI state) and sometimes `view/` subpackage.
- `presentation/search/mapper/` — presentation mappers for search filters/placeholders.
- `di/` — Hilt module.
- `navigation/` — feature destination, deep link handler, nav item.

## Key Components
- **ViewModels**: `CloudDriveViewModel`, `NewRubbishBinViewModel`, `IncomingSharesViewModel`, `OutgoingSharesViewModel`, `LinksViewModel`, `OpenPasswordLinkViewModel`, `OfflineViewModel`, `FavouritesViewModel`, `SearchViewModel`, `AudioViewModel`, `FolderLinkViewModel`, `FileLinkViewModel`, `DriveSyncViewModel`.
- **Use Cases**: None defined here — injected from `:domain` and shared modules.
- **Repositories / Gateways / Data sources**: None — this module has no data/domain layer.
- **Navigation**: `CloudDriveFeatureDestination` (implements `FeatureDestination`, registers all screen entries into the Navigation3 graph), `CloudDriveDeepLinkHandler` + `PasswordLinkDeepLinkHandler` (`DeepLinkHandler`), `DriveSyncNavItem` (`MainNavItem`). Per-screen `*ScreenDestination.kt` files expose `NavKey`-based entry-provider extensions (e.g. `cloudDriveScreen`, `rubbishBin`, `shares`).
- **UI**: Compose screens — `CloudDriveScreen`, `RubbishBinScreen`, `SharesScreen`, `OfflineScreen`, `FavouritesScreen`, `SearchScreen`, `AudioScreen`, `FolderLinkScreen`, `FileLinkScreen`, `DriveSyncScreen`. Presentation mappers: `TypeFilterToSearchMapper`, `SearchPlaceholderMapper`, `SearchFilterStringMapper`.

## Module Dependencies
Convention plugins: `mega.android.library`, `mega.android.library.compose`, `mega.android.hilt`, plus Kotlin serialisation.

Project deps: `:domain`, `:navigation`, `:core:navigation-contract`, `:core:coroutine`, `:core:transfers`, `:core:formatter`, `:core:feature-flags`, `:core:analytics:analytics-tracker`, `:core:ui-components:node-components`, `:core:ui-components:shared-components`, `:shared:ads`, `:shared:nodes`, `:shared:search`, `:shared:account`, `:shared:transfers`, `:feature:transfers:transfers-snowflake-components`, `:feature:sync`, `:resources:string-resources`, `:resources:icon-pack`.

Notable external libs: MEGA core-ui & analytics, Compose BOM, Navigation3 (`navigation3.runtime`/`navigation3.ui`), Material3 adaptive navigation suite, Hilt navigation, compose-state-events, ML Kit document scanner, Timber. Also consumes the pre-built MEGA SDK (`preBuiltSdkDependency`).

## Testing
JUnit5 + Mockito + Turbine + Truth (see root `.claude/CLAUDE.md` for global conventions). Hilt test, Navigation testing, and `:core-test`/`:core-ui-test`/`analytics-test` are available.

Run: `./gradlew feature:cloud-drive:cloud-drive:testDebugUnitTest`

## Notes & Gotchas
- Navigation uses **Navigation3** (`NavKey` + `EntryProviderScope`), not Navigation Compose. New screens add a `*ScreenDestination.kt` entry-provider extension and register it in `CloudDriveFeatureDestination`.
- Cross-feature navigation targets come from `:navigation` `mega.privacy.android.navigation.destination.*` NavKeys (e.g. `FileLinkNavKey`, `OfflineNavKey`, `TransfersNavKey`); screens receive `NavigationHandler` and `TransferHandler` rather than navigating directly.
- This module exposes nothing app-side directly: `CloudDriveModule` multibinds `FeatureDestination`, `MainNavItem`, and `DeepLinkHandler` `@IntoSet` so the host app discovers screens, the nav item, and deep links automatically.
- Note the rubbish bin ViewModel is named `NewRubbishBinViewModel`.
- Lint: `CoroutineCreationDuringComposition` and `ComposeUnstableCollections` are disabled; `abortOnError = true`.
