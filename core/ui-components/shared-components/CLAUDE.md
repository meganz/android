# CLAUDE.md

This file provides guidance to Claude Code when working in the `:core:ui-components:shared-components` module.

> Module path: `:core:ui-components:shared-components` · Build file: `core/ui-components/shared-components/shared-components.gradle.kts` · Namespace: `mega.privacy.android.core.sharedcomponents`

> ⚠️ **Deprecated module — do not add new code here.** Put new UI in the correct place instead: the owning `:feature:*` module if it's feature-specific, the relevant `:shared:*` module if it's shared across features, or the core UI library (`mega.core.ui`) if it's genuinely a core/design-system component. When you touch or reuse anything in this module, take the opportunity to migrate it to its proper home (and update its callers) rather than extending it here.

## Overview
A small library module of reusable Jetpack Compose components, handlers, and extensions shared across features. It builds on the `mega.core.ui` / `core-ui-tokens` design system (MegaText, MegaIcon, DSTokens, AndroidTheme) and provides cross-cutting UI such as account-security dialogs, a global request-status progress bar, snackbar/permission handling, and intent/bundle helpers.

The module is Compose- and Hilt-enabled and depends only on `:domain` (plus resource modules), so it sits below feature modules and can be consumed broadly without pulling in feature code.

## Architecture & Layout
Source root: `src/main/java/mega/privacy/android/core/sharedcomponents/`

- `container/` — `AppContainerProvider` interface for app-wide Compose container wrappers (session, theming, passcode, PSA).
- `dialog/` — Material 3 dialog composables (`Enable2FADialogView`, `SecurityUpgradeDialogView`).
- `requeststatus/` — `RequestStatusProgressContainer` / `RequestStatusProgressBarContent` composables, `RequestStatusProgressViewModel` (Hilt), and `model/RequestStatusProgressUiState`.
- `snackbar/` — `SnackBarHandler` interface and `MegaSnackbarDuration` enum (both **deprecated** in favor of `SnackbarEventQueue`).
- `handler/` — `rememberAppSettingsHandler` composable + `AppSettingsHandler` for permission-denied snackbar / app-settings flow.
- `extension/` — `ThemeMode.isDarkMode()` Compose extension.
- `Intent.kt` — `Bundle`/`Intent` `serializable`/`parcelable`/`parcelableArrayList` extensions and `Intent.canBeHandled()`.

## Key Components
- **UI components**:
  - `Enable2FADialogView` — M3 dialog prompting the user to enable two-factor authentication.
  - `SecurityUpgradeDialogView` — M3 dialog for the account security upgrade flow.
  - `RequestStatusProgressContainer` / `RequestStatusProgressBarContent` — animated linear progress bar driven by `MonitorRequestStatusProgressEventUseCase` via `RequestStatusProgressViewModel`.
  - `rememberAppSettingsHandler` — composable handler that shows a snackbar to open app settings on permission denial and re-checks permissions on return.
  - `AppContainerProvider` — interface for building app-wide Compose containers (implemented elsewhere).
  - `ThemeMode.isDarkMode()` — resolves dark/light from `ThemeMode` + system setting.

## Module Dependencies
- Project: `:resources:icon-pack`, `:resources:string-resources`, `:domain`.
- External: `mega.core.ui` + `core-ui-tokens` (design system), Compose BOM, Material 3 (incl. adaptive/window), Activity Compose, Coil 3, Timber.
- Test: `:core-test`, `:core-ui-test`, JUnit 5 BOM, UI-test / unit-test / JUnit5 bundles.

## Testing
JUnit 5 with Compose UI testing utilities (`:core-ui-test`). Unit tests run on `targetSdk = 34`.

Run: `./gradlew core:ui-components:shared-components:testDebugUnitTest`

## Notes & Gotchas
- **Deprecated — do not add new components here.** New UI belongs in the owning `:feature:*` module, the relevant `:shared:*` module, or the core UI library (`mega.core.ui`) for true core components. If a change forces you to work in here, migrate the affected pieces to their proper home and repoint their usages as part of the change.
- `SnackBarHandler` and `MegaSnackbarDuration` are deprecated — use `mega.privacy.android.navigation.contract.queue.SnackbarEventQueue` for new snackbar work.
- This module may only depend on `:domain` (not feature/data modules); keep additions free of feature-specific logic so it stays broadly reusable.
- Prefer design-system primitives (`MegaText`, `MegaIcon`, `DSTokens`, `AndroidTheme`) over raw Material 3 widgets and hardcoded colors; `SecurityUpgradeDialogView` still uses a hardcoded button color and is the exception, not the pattern to copy.
- Follow root `.claude/CLAUDE.md` for global conventions (4-space indent, naming, ViewModel/state patterns).
