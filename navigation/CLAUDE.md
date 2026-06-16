# CLAUDE.md

This file provides guidance to Claude Code when working in the `:navigation` module.

> Module path: `:navigation` · Build file: `navigation/navigation.gradle.kts` · Namespace: `mega.privacy.android.navigation`

## Overview
This module is the **navigation runtime / implementation** sitting between the app and the rest of the codebase. It declares the high-level navigation entry-point interfaces the app exposes to feature and legacy code (`MegaNavigator`, `AppNavigator`, `SettingsNavigator`), the concrete `NavKey` destinations used by the single-activity Navigation 3 host, the activity-result contract surface (`MegaActivityResultContract`), and the deep-link dispatch interfaces. It depends on `:core:navigation-contract` and consumes the marker `NavKey` interfaces defined there (e.g. `NoSessionNavKey`, `NoNodeNavKey`, `DialogNavKey`, `MainNavItemNavKey`).

Concretely: `:core:navigation-contract` holds the **contracts/abstractions** that feature modules implement to stay decoupled; `:navigation` holds the **app-facing navigator API and the catalog of concrete destinations** (the `NavKey`s for cloud drive, auth, settings, transfers, media, PDF, device center, etc.). Most interfaces here are implemented in the `:app` module; this module intentionally contains no `src/test`.

## Architecture & Layout
All code lives under `mega.privacy.android.navigation`:
- **(root)** — top-level navigator contracts and infra: `MegaNavigator` (= `AppNavigator` + `SettingsNavigator` + extras), `AppNavigator`, `MegaActivityResultContract`, the Hilt `@EntryPoint`s (`MegaNavigatorEntryPoint`, `MegaActivityResultContractEntryPoint`) with their `Context.megaNavigator` / `Context.megaActivityResultContract` accessors, deep-link interfaces (`DeeplinkHandler`, `DeeplinkProcessor`, `DefaultDeeplinkHandler`), and value/param holders (`OpenTextEditorParams`, `PendingDeepLink`, `ExtraConstant`).
- **destination/** — the concrete `@Serializable` `NavKey` destinations (auth, cloud drive, settings, transfers, media, PDF viewer, device center, document scanner, contacts, consent, deep links, legacy core activity/fragment bridges, dialogs, etc.). Many are also `@Parcelize`.
- **extensions/** — Compose/runtime helpers: `rememberMegaNavigator()`, `rememberMegaResultContract()`, and `serializableNavType()` / `typeMapOf()` for kotlinx-serialization-backed `NavType`s.
- **settings/** — `SettingsNavigator` and its `arguments/TargetPreference`.
- **camera/**, **payment/** — small arg/param types (`CameraArg`, `UpgradeAccountSource`).

## Key Components
- **`MegaNavigator`** — the single aggregate navigator interface (`AppNavigator` + `SettingsNavigator` + `launchMegaActivityIfNeeded`, `openHomeScreen`). The app provides the implementation; non-Hilt callers reach it via `Context.megaNavigator` (backed by `MegaNavigatorEntryPoint` + a cached `AtomicReference`), Compose callers via `rememberMegaNavigator()`.
- **`AppNavigator`** — the large surface of imperative `open*` / `launch*` calls (cloud drive, chat, upgrade, syncs, media player, PDF/image/text viewers, file info, achievements, etc.), plus `getPendingIntentWithDestination` which builds a `PendingIntent` targeting the single activity with a `NavKey & Parcelable` destination.
- **`MegaActivityResultContract`** — centralized `ActivityResultContract`s (move/copy folder, share folder, send-to-chat, name collision, in-app camera, add-to-album, video-to-playlist, etc.).
- **`NavKey` destinations** (`destination/`) — concrete serializable destinations consumed by the Navigation 3 host. `HomeScreensNavKey` is notable: it serialises a `MainNavItemNavKey` root + a `List<NavKey>` stack to/from JSON (via `NavKeySerializer`) so the home back stack can be carried as parcelable/serializable args.
- **Deep-link dispatch** — `DeeplinkProcessor` (match + execute one rule), `DeeplinkHandler` (match/process), and `DefaultDeeplinkHandler` which fans a deeplink out across an injected `Set<DeeplinkProcessor>`.
- **`serializableNavType()` / `typeMapOf()`** (`extensions/NavType.kt`) — generic kotlinx-serialization `NavType<T>` builders for passing complex typed args through Navigation.

## Module Dependencies
- Project: `:domain`, `:core:navigation-contract`.
- External: `androidx.navigation.compose`, **`androidx.navigation3.runtime`** (Navigation 3, source of `NavKey`), `androidx.appcompat`, `lib.mega.analytics`.
- Convention plugins: `mega.android.library`, `mega.android.library.compose`, `mega.android.hilt`, plus `kotlin-parcelize` and the Kotlin serialization plugin.

## Testing
JUnit 5 (with the standard UI/unit test bundles). Run:
`./gradlew navigation:testDebugUnitTest`
Note: this module currently has no `src/test` — it is mostly interfaces and serializable destinations; behaviour lives in the implementing module (`:app`).

## Notes & Gotchas
- **Contract vs. implementation**: don't confuse this with `:core:navigation-contract`. That module holds the feature-facing contracts (`FeatureDestination`, `MainNavItem`, `NavigationHandler`, marker `NavKey`s, scenes/metadata). This module holds the app-facing navigator API and the concrete destination catalog. New destination *requirement* markers go in the contract; new concrete app destinations and navigator methods go here.
- `MegaNavigator`/`MegaActivityResultContract` are **interfaces only** here; implementations live in `:app`. The `*EntryPoint` + `Context.mega*` accessors exist specifically so non-Hilt classes can obtain them.
- `HomeScreensNavKey` round-trips its root + destination stack through JSON; when adding fields, keep them serializable and remember the `timestamp` exists to force recomposition for an otherwise-identical key.
- Destinations are `@Serializable` (and often `@Parcelize`) — these are required by the serialization-backed `NavType`s and by `getPendingIntentWithDestination` (which requires `NavKey & Parcelable`).
