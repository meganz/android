# CLAUDE.md

This file provides guidance to Claude Code when working in the `:feature:devicecenter` module.

> Module path: `:feature:devicecenter` · Build file: `feature/devicecenter/devicecenter.gradle.kts` · Namespace: `mega.privacy.android.feature.devicecenter`

## Overview
This module implements the **Device Center** feature: a screen that lists the user's own device plus other devices, each containing backup / sync / camera-upload folders, along with their connection and sync status. It supports per-folder/device actions (rename device, info, add new backup/sync, camera uploads, remove connection) surfaced through a bottom sheet, plus an info screen and a rename-device dialog.

It is a Compose-first feature module (also retaining a `DeviceCenterFragment` host) wired into the app via Navigation3 destinations and Hilt. Global project conventions live in the root `.claude/CLAUDE.md`.

## Architecture & Layout
Follows the standard layering under `mega.privacy.android.feature.devicecenter`:
- `domain/` — feature-local `entity/` models (`DeviceCenterNode`, `DeviceNode`, `DeviceFolderNode`, status enums), `usecase/` (incl. `usecase/mapper/` domain mappers and a `usecase/folder/` use case impl).
- `ui/` — Compose presentation: ViewModels, `view/` composables, `lists/`, `bottomsheet/` (with `body/` and `tiles/`), `renamedevice/`, `model/` UI state + UI node models (`model/icon/`, `model/status/`), and `mapper/` (domain → UI node mappers).
- `navigation/` — Navigation3 destinations, feature destination registration, and deep link handling.
- `di/` — Hilt module.

## Key Components
- **ViewModels**: `DeviceCenterViewModel`, `DeviceCenterInfoViewModel`, `RenameDeviceViewModel` (in `ui/`, `ui/renamedevice/`).
- **Use Cases**: `GetDevicesUseCase` (feature-local); `RemoveDeviceFolderConnectionUseCaseImpl` implements the domain `RemoveDeviceFolderConnectionUseCase`. `DeviceCenterViewModel` also consumes shared domain use cases (`IsCameraUploadsEnabledUseCase`, `MonitorConnectivityUseCase`, `RemoveDeviceFolderConnectionUseCase`).
- **Repositories / Gateways / Data sources**: None defined in this module — data access is via injected domain use cases and the `:data` / `:domain` modules.
- **Mappers**: Domain mappers (`DeviceNodeMapper`, `DeviceFolderNodeMapper`, `DeviceNodeStatusMapper`) and UI mappers (`DeviceCenterUINodeStatusMapper`, `DeviceUINodeListMapper`, `DeviceFolderUINodeListMapper`, `DeviceUINodeIconMapper`, `DeviceFolderUINodeIconMapper`).
- **Navigation**: `DeviceCenterScreenDestination.kt` registers `EntryProviderScope<NavKey>.deviceCenterScreen(...)` bound to the shared `DeviceCenterNavKey` (from `:navigation`); `DeviceCenterFeatureDestination`, `DeviceCenterDeepLinkHandler`, and info-screen routes (`DeviceCenterInfoScreenRoute` / `...RouteM3`).
- **UI**: `DeviceCenterScreen` / `DeviceCenterScreenM3`, `DeviceCenterContent`, app bars, empty/no-network/nothing-setup states, list items, loading screen, bottom sheet bodies + action tiles, `RenameDeviceDialog`, and `DeviceCenterInfoScreen`.

## Module Dependencies
Project modules: `:domain`, `:data`, `:navigation`, `:core:navigation-contract`, `:core:formatter`, `:core:analytics:analytics-tracker`, `:shared:original-core-ui`, `:shared:sync`, `:legacy-core-ui`, `:resources:string-resources`, `:resources:icon-pack`, `:lint` (lintChecks). Tests use `:core-test`, `:core-ui-test`.

Plugins: `mega.android.library`, `mega.android.library.compose`, `mega.android.hilt`, Kotlin serialization. Notable external libs: Jetpack Compose (BOM + Material3), Navigation3 runtime, Hilt navigation, Lifecycle (viewmodel/service/runtime-compose), `mega.analytics`, Timber, `compose.state.events`, and the prebuilt MEGA SDK (`preBuiltSdkDependency`).

## Testing
Unit tests live in `src/test`. Use JUnit 5 + Mockito + Turbine (Flow/UiState) + Truth, per root conventions.

Run: `./gradlew feature:devicecenter:testDebugUnitTest`

## Notes & Gotchas
- This module defines feature-local domain entities/use cases AND consumes shared `:domain` use cases — check both `feature.devicecenter.domain.usecase` and `domain.usecase.*` before adding a new one.
- Two UI variants exist (legacy and `*M3`/Material3, e.g. `DeviceCenterScreenM3`, `DeviceCenterAppBarM3`, `DeviceCenterInfoScreenM3`); confirm which path you are modifying.
- Navigation uses Navigation3 (`EntryProviderScope`/`NavKey`) and the shared `DeviceCenterNavKey` from `:navigation` — register destinations via the feature destination, not hardcoded routes.
- The Hilt module (`DeviceCenterModule`) is an `internal interface` mixing `@Binds` and `@Provides`; keep bindings `internal`.
- Lint check `CoroutineCreationDuringComposition` is disabled and `abortOnError = true`, so other lint failures break the build.
