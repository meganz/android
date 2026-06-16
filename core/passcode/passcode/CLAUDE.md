# CLAUDE.md

This file provides guidance to Claude Code when working in the `:core:passcode:passcode` module.

> Module path: `:core:passcode:passcode` · Build file: `core/passcode/passcode/passcode.gradle.kts` · Namespace: `mega.privacy.android.core.passcode`

## Overview
This module implements the app-wide passcode / PIN lock security feature. It enforces a passcode (or biometric) unlock screen when the app returns to the foreground, based on the user's configured passcode type and timeout. It hooks into the process and activity lifecycle to decide when locking is required, renders the unlock UI in Compose, and validates entered passcodes against the domain layer.

It also provides utilities so that legitimate external activity launches (file/folder pickers, document scanner, in-app updates, share targets) do NOT incorrectly trigger the passcode prompt when the user returns.

## Architecture & Layout
Compose UI feature module following the project's clean-architecture split. Business logic and use cases live in `:domain`; this module holds presentation and lifecycle integration only.

- `presentation/` — `PasscodeUnlockViewModel`, mappers, models, navigation destination, and Compose views.
- `check/` — `PasscodeCheckViewModel` + `PasscodeCheckState` for observing the lock state.
- root package — lifecycle hooks, the facade, and activity-result/launcher helpers (`PasscodeFacade`, `PasscodeLifecycleDispatcher`, `PasscodeProcessLifeCycleOwner`, `PasscodeLifeCycleObserver`, `PasscodeAwareContract`, `RememberPasscodeAwareLauncher`).

## Key Components
- **ViewModels**: `PasscodeUnlockViewModel` (drives the unlock screen: passcode type, attempts, theme; calls `UnlockPasscodeUseCase`), `PasscodeCheckViewModel` (maps `MonitorPasscodeLockStateUseCase` into `PasscodeCheckState.Locked/UnLocked`).
- **Use Cases**: consumed from `:domain` — `UnlockPasscodeUseCase`, `MonitorPasscodeTypeUseCase`, `MonitorPasscodeAttemptsUseCase`, `MonitorPasscodeLockStateUseCase`, plus `MonitorThemeModeUseCase`. No use cases are defined in this module.
- **Repositories / Data sources**: none — state is sourced via domain use cases.
- **UI**: `PasscodeView` (unlock screen, registered as `PasscodeNavKey` / `passcodeView` Navigation3 entry with overlay suppression), `PasscodeLoadingView`, `BiometricAuthPrompt`. Mapper `PasscodeTypeMapper`; models `PasscodeUIType`, `PasscodeUnlockState`.

## Module Dependencies
- `:domain`, `:core:passcode:passcode-snowflake-components`, `:core:analytics:analytics-tracker`, `:core:navigation-contract`, `:core:coroutine`, `:resources:string-resources`, `:lint` (lintChecks).
- External: Timber, mega.analytics, mega core-ui (DSTokens), AndroidX appcompat, biometric, hilt-navigation, lifecycle (runtime + compose), navigation3 runtime, compose activity + BOM bundles, Material3. Plugins: `mega.android.library`, `mega.android.library.compose`, `mega.android.hilt`, kotlin serialization.

## Snowflake Components
`:core:passcode:passcode-snowflake-components` contains the standalone, reusable Compose input control `PasscodeField` (`mega.privacy.android.core.passcode.components.PasscodeField`) — a masked, fixed-length character input with an `onComplete` callback, configurable `numberOfCharacters`, `maskCharacter`, `keyboardOptions`, and per-cell composable. Test tag: `PASSCODE_FIELD_TAG`. Used by the unlock UI in the parent module. Do not add a separate CLAUDE.md for this submodule.

## Testing
JUnit 5 + Mockito + Turbine + Truth (+ Compose UI test, Hilt test, `:core-test`, `:core:analytics:analytics-test`). Run: `./gradlew core:passcode:passcode:testDebugUnitTest`.

## Notes & Gotchas
- `PasscodeLifecycleDispatcher.init(context)` is idempotent (guarded by an `AtomicBoolean`) and must be initialized early to register activity lifecycle callbacks.
- When launching external activities that should not trigger the lock screen on return, use `rememberPasscodeAwareLauncher` / `ActivityResultContract.withPasscodeAwareness()` instead of `rememberLauncherForActivityResult` — they call `PasscodeProcessLifecycleOwner.skipNextPasscodeCheck()`. Add new external intent actions to `PasscodeAwareContract.EXTERNAL_ACTIONS`.
- `PasscodeFacade` (implements the legacy `PasscodeCheck` interface) lets a host activity temporarily disable locking (`disablePasscode` / `enablePassCode`); it is `@ActivityContext`-scoped.
- Global conventions (naming, DI, 4-space indent, test method naming) live in the root `.claude/CLAUDE.md`.
