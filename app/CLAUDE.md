# CLAUDE.md

This file provides guidance to Claude Code when working in the `:app` module.

> Module path: `:app` · Build file: `app/app.gradle.kts` · Namespace: `mega.privacy.android.app`

## Overview
`:app` is the main Android application module and app shell. It hosts the `@HiltAndroidApp` Application, the root Activity, app-level dependency injection, deep-link/intent handling, FCM/notification services, and a large body of legacy XML/View + early-Compose screens (`presentation/*`).

This module is very large (~3000+ Kotlin files) and largely legacy. Most new feature code now lives in dedicated `:feature:*` modules, with shared logic in `:domain`, `:data`, `:shared:*`, and `:core:*`. The app module increasingly acts as a composition root: it aggregates feature navigation, wires Hilt graphs, and provides app-wide infrastructure rather than housing new feature implementations.

## Architecture & Layout
- `MegaApplication.kt` — the `@HiltAndroidApp` Application (`MultiDexApplication`, `Configuration.Provider`, Coil `SingletonImageLoader.Factory`). Injects the MEGA SDK gateways (`MegaApiAndroid`, `MegaChatApiAndroid`) and the legacy `DatabaseHandler`.
- `appstate/` — newer Navigation3-based app shell. `MegaActivity` (`@AndroidEntryPoint`, `FragmentActivity`) calls `setContent { ... MegaNavDisplay(...) }` to render the root nav graph; `appstate/content/navigation/` builds the top-level backstack and aggregates destinations.
- `main/`, `presentation/` — legacy and transitional screens (Activities, Fragments, ViewModels) for cloud drive, file explorer, contacts, settings, etc. ~100 `*Activity.kt` files exist; many are legacy.
- `di/` — app-level Hilt modules and entry points (see below).
- Supporting packages: `fcm/`, `services/`, `receivers/`, `notifications/`, `deeplinks/`, `nav/`, `globalmanagement/`, `initializer/`, `providers/`, `middlelayer/`, `utils/`.

## Key Components
- **Application / Activities**: `MegaApplication` (Hilt application + SDK/DB injection). `appstate/MegaActivity` is the Navigation3 Compose host; legacy flows still launch standalone Activities (e.g. `FileExplorerActivity`, `ContactFileListActivity`).
- **Navigation**: feature destinations are contributed as a Hilt `@IntoSet` multibinding of `mega.privacy.android.navigation.contract.FeatureDestination` (see `di/navigation/FeatureDestinationModule.kt`, ~14 bindings — meeting, notifications, pdfviewer, document-scanner, settings, psa, logout, plus `LegacyCoreActivityFeatureGraph` for legacy Activity-based flows). `MegaNavDisplay` renders the aggregated graph.
- **DI**: many `@InstallIn(SingletonComponent::class)` modules under `di/` — e.g. `AppModule`, `GatewayModule`, `DbHandlerModule`, `BillingModule`, `WorkManagerModule`, `CoroutineDispatchersModule`/`CoroutineScopesModule`, `MapperModule`, plus feature-grouped subpackages (`chat/`, `transfers/`, `photos/`, `meeting/`, `sync/`, `navigation/`). Entry points include `DatabaseEntryPoint`, `DomainNameEntryPoint`, `EntryPointsModule`.

## Module Dependencies
Depends on `:domain`, `:data`, and the prebuilt MEGA SDK, plus broad groups of modules:
- **Feature modules**: `:feature:*` — chat, sync, transfers, payment, home, photos, cloud-drive, devicecenter, cloudexplorer, document-scanner, video-editor, pdfviewer, text-editor, myaccount, contact, sign-in-external, notifications.
- **Shared**: `:shared:*` — ads, nodes, search, account, contact, sync, original-core-ui.
- **Core**: `:core:*` — formatter, coroutine, feature-flags, transfers, passcode, navigation-contract, analytics, ui-components/node-components, ui-components/shared-components, plus `*-snowflake-components`.
- **Navigation / resources / misc**: `:navigation`, `:resources:icon-pack`, `:resources:string-resources`, `:legacy-core-ui`, `:third-party-lib:twemoji`, `:baselineprofile`.
- **Test**: `:core-test`, `:core-ui-test`, `:core:analytics:analytics-test`.

## Testing
JUnit 5 + Mockito + Turbine + Truth (see root `.claude/CLAUDE.md` for conventions). Run unit tests with the `gms` flavor:
```
./gradlew app:testGmsDebugUnitTest
```

## Notes & Gotchas
- Very large legacy module — prefer adding new code to the relevant `:feature:*` / `:shared:*` / `:core:*` module, not here. Use `:app` only for app shell, composition, navigation aggregation, and app-wide infrastructure.
- Mixed UI stack: legacy XML/View + View Binding alongside Compose and Navigation3. The `appstate` package is the modern path; `main`/`presentation` is largely legacy.
- Single product flavor `gms` (dimension `service`); build types `debug`, `release`, `qa` (qa uses `.qa` applicationId suffix). Always include the flavor in task names (e.g. `GmsDebug`).
- Convention plugins: `mega.android.app`, `mega.android.application.compose`, `mega.android.application.firebase`, `mega.android.hilt`. Custom lint via `:lint`.
- New feature navigation must be exposed as a `FeatureDestination` and contributed via `@IntoSet` so `MegaNavDisplay` picks it up.
