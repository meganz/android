# CLAUDE.md

This file provides guidance to Claude Code when working in the `:feature:chat` module.

> Module path: `:feature:chat` · Build file: `feature/chat/chat.gradle.kts` · Namespace:
`mega.privacy.android.feature.chat`

## Overview
`:feature:chat` is a Jetpack Compose feature module covering chat-adjacent functionality: meetings (call recording, recording-consent, "meeting has ended"), and call/chat settings. It is presentation-focused — it consumes business logic from `:domain` use cases and exposes screens, dialogs, and navigation entry points to the host app via the navigation contract.

The module wires itself into the app through Hilt multibindings (`@IntoSet`): it contributes a `FeatureDestination`, a `DeepLinkHandler`, and a `PostLoginInitialiser`. There is no data layer in this module (no repositories/gateways/mappers); state is sourced from injected use cases.

## Architecture & Layout
Top-level package: `mega.privacy.android.feature.chat`.
- `meeting/` — meeting features. `recording/` (ViewModels, `model/` UI state, `view/` Compose), `recording/initialiser/`, `call/` (extensions), `view/` (dialogs).
- `settings/` — `calls/` call settings (Fragment, ViewModel, `model/` UI state, `view/` Compose screen) and `settings/navigation/` (legacy navigation graph + destination).
- `navigation/` — feature entry points: `ChatFeatureDestination` (Navigation3 `FeatureDestination`), `ChatDialogDestinations` (dialog graph), `ChatsDeepLinkHandler`, and `*NavKey` keys.
- `di/` — `ChatModule` Hilt module.

## Key Components
- **ViewModels**: `CallRecordingViewModel`, `CallRecordingConsentDialogViewModel`, `CallSettingsViewModel`.
- **Use Cases**: None defined here; the module injects use cases from `:domain`.
- **Repositories / Gateways / Data sources**: None — no data layer in this module.
- **Navigation**: `ChatFeatureDestination` and `ChatDialogDestinations` (Navigation3 entry providers), `ChatsDeepLinkHandler` (`DeepLinkHandler`), `CallRecordingConsentDialogNavKey`, `MeetingHasEndedDialogNavKey`; legacy `CallSettingsNavigationDestination` / `CallSettingsNavigationGraph`.
- **UI**: `CallSettingsScreen`, `CallRecordingConsentDialog` / `CallRecordingConsentView`, `MeetingHasEndedDialog`; `CallSettingsFragment` (legacy Fragment host). UI state: `CallRecordingUIState`, `CallRecordingConsentUiState`, `CallSettingsUiState`.

## Module Dependencies
Project modules: `:domain`, `:navigation`, `:core:navigation-contract`, `:core:coroutine`, `:resources:icon-pack`, `:resources:string-resources`. Tests: `:core-test`, `:core-ui-test`.
Notable external libs: MEGA core-ui, Material3 (+ window size class), Compose BOM, Hilt navigation, Navigation Compose, Navigation3 runtime/ui, Kotlin Serialization, Timber.

## Testing
JUnit 5 + Mockito + Turbine (Flow/UiState) + Truth, with Hilt test support. Run: `./gradlew feature:chat:testDebugUnitTest`. Unit-test `targetSdk` is pinned to 34.

## Notes & Gotchas

- The `namespace` is `mega.privacy.android.feature.chat`. The module has no `res/` of its own and
  uses `mega.privacy.android.shared.resources.R`.
- Build plugins: `mega.android.library`, `mega.android.library.compose`, `mega.android.room`, `mega.android.hilt`, plus Kotlin serialization. The Room plugin is applied even though no Room entities/DAOs currently live here.
- Entry points are registered via Hilt `@IntoSet` multibindings in `ChatModule` — add new destinations/handlers there, not in the app module.
- Mix of Navigation3 (`FeatureDestination`, `NavKey`) for new screens/dialogs and a legacy fragment + nav-graph path for call settings.
- Global conventions (Clean Architecture, naming, formatting) live in the root `.claude/CLAUDE.md`.
