# CLAUDE.md

This file provides guidance to Claude Code when working in the `:core:navigation-contract` module.

> Module path: `:core:navigation-contract` · Build file: `core/navigation-contract/navigation-contract.gradle.kts` · Namespace: `mega.privacy.android.navigation.contract`

## Overview
This module defines the navigation **contracts and abstractions** that let feature modules expose their screens (destinations) to the app's single-activity navigation host without depending on each other or on the `:app` module. It contains almost exclusively interfaces, abstract classes, and small infrastructure helpers — no concrete feature screens.

Navigation is built on **Jetpack Navigation 3** (`androidx.navigation3.runtime` / `ui`). Features contribute their navigation graphs by implementing the contract interfaces here and injecting them into Hilt multibinding sets. The app collects those sets and assembles the full back stack / entry providers at runtime, keeping features fully decoupled.

## Architecture & Layout
All code lives under `mega.privacy.android.navigation.contract`:
- **(root)** — top-level contracts: `FeatureDestination`, `MainNavItem`, `NavDrawerItem`, `NavigationHandler`, `NavigationResultsHandler`, `NavigationUiController`, `TransferHandler`, `NavOptions`, `PreferredSlot`.
- **navkey/** — marker `NavKey` interfaces describing destination requirements: `NoSessionNavKey` (`Optional`/`Mandatory`), `NoNodeNavKey`, `MainNavItemNavKey`, `ContinuousScanNavKey`, `Suppressable`.
- **dialog/** — `AppDialogDestinations`, `DialogNavKey`.
- **home/** — Home screen widget contract: `HomeWidget`, `HomeWidgetProvider`, `HomeWidgetOrder`.
- **metadata/** — `buildMetadata { }` DSL and `NavEntryMetadataScope` for declarative `NavEntry` metadata.
- **shared/** — `SharedViewModelStoreNavEntryDecorator`, `sharedViewModel()`, and the `withSharedViewModelStoreKey` / `provideSharedViewModelScope` metadata helpers for cross-module shared ViewModel scopes.
- **queue/** — event queues for a single-activity setup: `NavigationEventQueue`, `NavPriority`, `QueueEvent`; `snackbar/` (`SnackbarEventQueue` + receiver) and `dialog/` (`AppDialogsEventQueue`, `AppDialogEvent`).
- **suppression/** — overlay suppression: `SuppressionType`, `OverlaySuppressionState`, `OverlaySuppressionNavEntryDecorator`, `OverlaySuppressionMetadata`.
- **bottomsheet/**, **transparent/** — Navigation 3 `SceneStrategy`/`Scene` implementations + metadata for modal bottom sheet and transparent destinations.
- **deeplinks/** — `DeepLinkHandler` abstract class for feature-contributed deep link resolution.
- **settings/** — `FeatureSettings`, `SettingEntryPoint`.
- **featureflag/** — `FeatureFlagGate` composable and `FeatureFlagResolver` / `GetFeatureFlagValueEntryPoint`.
- **initialisation/** — `AppStartInitialiser`(`Action`), `PostLoginInitialiserAction` for multi-injected startup hooks.
- **menu/**, **state/**, **transition/**, **qualifier/** — supporting contracts (`CommonMenuAction`, `LocalBottomNavigationVisible`, `NavTransition`, `@DefaultStartScreen`, etc.).

## Key Components
- **Navigation contracts**:
  - `FeatureDestination` — a feature's entry point; exposes `navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit`. Injected into a multibinding set.
  - `MainNavItem` — a bottom-nav / main destination (icon, label, badge `Flow`, `PreferredSlot`, analytics id) plus its `EntryProviderScope` screen lambda. `Iterable<MainNavItem>.sortedByPreferredSlot()` orders them.
  - `NavDrawerItem` — sealed nav-drawer entries (`Account`, `PrivacySuite`).
  - `NavigationHandler` — runtime navigation API passed into graphs (`navigate`, `back`, `backTo`, `navigateAndClearBackStack`, `navigateAndClearTo`); extends `NavigationResultsHandler`. `TransferHandler` triggers transfer events; `NavigationUiController` toggles chrome visibility.
  - `NavKey` markers — `NoSessionNavKey.Optional/Mandatory`, `NoNodeNavKey`, `Suppressable` declare login/state requirements consumed by the host and `DeepLinkHandler`.
  - `AppDialogDestinations` / `DialogNavKey` — dialog destinations contributed the same way as screens.
  - `HomeWidget` / `HomeWidgetProvider` — Home screen widget contract (single widget vs. provider of multiple).
  - `DeepLinkHandler` — abstract base for resolving a `Uri` to `List<NavKey>`, with login-state checks and snackbar fallbacks.
  - Metadata + scenes — `buildMetadata`, `SharedViewModelStoreNavEntryDecorator`/`sharedViewModel`, `BottomSheetSceneStrategy`, `TransparentSceneStrategy`, `OverlaySuppression*`.

## Module Dependencies
- Project: `:domain`, `:resources:string-resources`, `:resources:icon-pack`.
- External: `androidx.navigation3.runtime` + `androidx.navigation3.ui`, `androidx.navigation.compose`, `androidx.hilt.navigation`, `androidx.material3`, Hilt (`google.hilt.android`, `javax.inject`), `lib.mega.core.ui`, `lib.mega.analytics`, Timber.
- Convention plugins: `mega.android.library`, `mega.android.library.compose`, `mega.android.hilt`, plus `kotlin-parcelize`.

## Testing
JUnit 5 with Truth and Robolectric (`testlib.compose.junit` for composables). Run:
`./gradlew core:navigation-contract:testDebugUnitTest`

## Notes & Gotchas
- **Decoupling pattern**: features never reference each other's `NavKey`s. They implement `FeatureDestination` / `MainNavItem` / `AppDialogDestinations` / `HomeWidget` / `DeepLinkHandler` etc. and bind them into Hilt **multibinding sets** (`@IntoSet`); the app aggregates the sets and builds the Navigation 3 entry providers. To add a destination type, add a contract interface here, then collect it in the app host.
- `NavigationHandler` does **not** support enter/exit transition options in `NavOptions` — only back-stack manipulation (e.g. `popUpTo`). Transitions are handled via `transition/` / scene strategies.
- **Cross-module shared ViewModels**: prefer the named-scope pattern — provider entry calls `provideSharedViewModelScope("scopeName")`, consumer calls `withSharedViewModelStoreKey("scopeName")` inside `buildMetadata { }`, then both use `sharedViewModel<VM>()`. The shared store is cleared when the provider entry is popped. Requires `SaveableStateHolderNavEntryDecorator` to be installed for `SavedStateHandle` support.
- Enum entries use PascalCase per the root `.claude/CLAUDE.md` conventions (e.g. `NavPriority`, `SuppressionType` cases).
