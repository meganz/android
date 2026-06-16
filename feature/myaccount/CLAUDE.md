# CLAUDE.md

This file provides guidance to Claude Code when working in the `:feature:myaccount` module.

> Module path: `:feature:myaccount` · Build file: `feature/myaccount/myaccount.gradle.kts` · Namespace: `mega.privacy.android.feature.myaccount`

## Overview
This module provides the "My Account" Home-screen widget. It surfaces the user's avatar, name, account type, and storage/quota usage as a compact, clickable card on the Home screen, and routes the user to the appropriate account/upgrade destination when tapped.

It is a presentation-only feature module: it contains no domain or data layer of its own. All business logic comes from injected use cases in `:domain`, and the widget is contributed to the Home screen via the `HomeWidget` extension point from `:core:navigation-contract`.

## Architecture & Layout
Single top-level package `mega.privacy.android.feature.myaccount`, all under `presentation` plus a `di` package:

- `di/` — Hilt modules wiring the widget and mappers.
- `presentation/widget/` — the widget entry point, ViewModel, and Compose UI.
- `presentation/widget/view/` — small reusable composables (`AvatarView`, `MyAccountHorizontalProgressBar`).
- `presentation/mapper/` — presentation mappers (avatar content, account-type name, quota level).
- `presentation/model/` — UI models (`MyAccountWidgetUiState`, `AvatarContent`, `QuotaLevel`).

Follows the global Clean Architecture / Hilt / Compose conventions in the root `.claude/CLAUDE.md`; no module-specific deviations.

## Key Components
- **ViewModels**: `MyAccountWidgetViewModel` — collects `MonitorAccountDetailUseCase`, `MonitorStorageStateUseCase`, avatar (`GetMyAvatarFileUseCase`, `GetMyAvatarColorUseCase`) and `GetUserFirstNameUseCase`, exposing `uiState: StateFlow<MyAccountWidgetUiState>` via `asUiStateFlow`.
- **Use Cases**: None defined here — all consumed from `:domain`.
- **Repositories / Gateways / Data sources**: None — this module has no data layer.
- **Navigation**: No NavKeys/Destinations. Integrates via the `HomeWidget` contract from `:core:navigation-contract`; `MyAccountHomeWidget` implements `HomeWidget` and is contributed `@IntoSet` in `MyAccountModule`. It navigates through the `NavigationHandler` passed into `DisplayWidget`.
- **UI**: `MyAccountHomeWidget` (`HomeWidget` impl, hosts `hiltViewModel`), `MyAccountWidget` (stateless Compose content + shimmer/loading state), and view helpers `AvatarView`, `MyAccountHorizontalProgressBar`.
- **Mappers**: `AvatarContentMapper` (fun interface, impl `AvatarContentMapperImpl`), `AccountTypeNameMapper` (a `SharedStringResourceProvider<AccountType?>`), `QuotaLevelMapper`. Bound via `AvatarMapperModule` / `AccountTypeNameMapperModule`.

## Module Dependencies
Project modules: `:navigation`, `:core:navigation-contract`, `:core:coroutine`, `:domain`, `:data`, `:resources:icon-pack`, `:resources:string-resources`, `:shared:original-core-ui`, `:core:ui-components:shared-components`, `:core:formatter`, `:third-party-lib:twemoji`, `:core:analytics:analytics-tracker`.

External: Compose BOM + Navigation3 runtime, `androidx.hilt.navigation`, Material3 adaptive navigation-suite, Kotlin serialization, MEGA analytics & core-ui, Timber, Coil3.

Plugins: `mega.android.library`, `mega.android.library.compose`, `mega.android.hilt`, Kotlin serialization.

## Testing
JUnit 5 + Mockito + Turbine + Truth, via `:core-test` and `:core-ui-test`. Run with:

```
./gradlew feature:myaccount:testDebugUnitTest
```

## Notes & Gotchas
- The widget is registered through Hilt `@IntoSet` multibinding into the `HomeWidget` set — it is not navigated to directly; do not add NavKeys for it.
- `MyAccountHomeWidget` exposes fixed contract flags (`canDelete = false`, `isDraggable = false`, `isConfigurable = true`, `defaultOrder = HomeWidgetOrder.MyAccount`); keep these in sync with Home-screen expectations.
- Navigation/analytics happen inside `DisplayWidget` based on `uiState` (e.g. quota level / business account) — keep tap-target decisions in the widget, not the stateless `MyAccountWidget`.
- Avatar loading emits twice (cached then force-refreshed); preserve that pattern to avoid avatar flicker/staleness.
- `targetSdk = 34` is pinned for unit tests in the build file.
