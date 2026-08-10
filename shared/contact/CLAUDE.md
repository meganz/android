# CLAUDE.md

This file provides guidance to Claude Code when working in the `:shared:contact` module.

> Module path: `:shared:contact` · Build file: `shared/contact/contact.gradle.kts` · Namespace: `mega.privacy.android.shared.contact`

## Overview
`:shared:contact` is a small, dependency-light library of reusable contact presentation building blocks: Compose UI components for rendering a contact (avatar, status, row), the presentational UI-state models they consume, and the mappers that convert domain entities (`ContactItem`, `ChatAvatarItem`) into those models. It deliberately holds no domain logic, no `ViewModel`, and no navigation — just stateless pieces that any feature can drop into its own screens.

This differs from `:feature:contact:contact` (namespace `mega.privacy.android.feature.contact`), which is a self-contained, end-to-end contact *feature* (screens, ViewModels, navigation). `:shared:contact` is the lower-level toolkit those features build on. There is also a sibling `:feature:contact:contact-snowflake-components` module — note that some KDoc references an older `ContactItemView` location there; the canonical `ContactItemView` now lives in this module.

## Architecture & Layout
Single source root: `src/main/java/mega/privacy/android/shared/contact/`.

- `components/` — Stateless `@Composable` UI built on `mega.android.core.ui` (core-ui).
- `model/` — `@Stable` presentational UI-state data classes. No Android `Context`, no resource IDs, no domain types leaking through.
- `mapper/` — `@Inject`-constructed mappers (`operator fun invoke`) translating domain entities into `model/` types.

Status text is intentionally *not* resolved here: mappers leave subtitle/status strings to the caller so the module stays free of `Context` and `stringResource` resolution.

## Key Components
**UI (`components/`)**
- `ContactItemView` — full contact row (avatar + name + status/last-seen, optional selection checkbox, optional kebab "more" action). Consumes a `ContactItemUiState`.
- `ContactAvatar` — single avatar rendering `AvatarData` (photo file or coloured initials), with optional verified badge and avatar-click handling. Exposes test tags (`CONTACT_ITEM_VIEW_AVATAR`, ...).
- `MultiAvatarView` — overlapping avatars for group/chat contexts.

**Models (`model/`)**
- `ContactItemUiState` — pre-resolved data for one contact row (`handle`, `displayName`, `status`, `lastSeen`, `avatar`, `isVerified`, `email`).
- `AvatarData` — sealed interface: `Image(file)` or `Initials(initials, avatarColor)`.
- `ContactPermissionUiState` — wraps a `ContactItemUiState` with an `AccessPermission` for share-permission UIs.

**Mappers (`mapper/`)**
- `ContactItemUiStateMapper` — `ContactItem` → `ContactItemUiState` (delegates to the status and avatar mappers; resolves display name from alias → full name → email).
- `ContactItemAvatarMapper` / `ChatAvatarItemMapper` — domain entity → `AvatarData`.
- `ContactItemStatusMapper` — `UserChatStatus` → core-ui `ContactItemStatus`.
- `ContactPermissionUiStateMapper` — builds `ContactPermissionUiState`.

## Module Dependencies
Project modules: `:domain`, `:resources:string-resources`, `:resources:icon-pack`, `:third-party-lib:twemoji`.

Notable external libs: core-ui + core-ui-tokens (`lib.mega.core.ui*`), Compose BOM + Material 3, `vdurmont.emoji`, Timber, `javax.inject`. Test deps include `:core-ui-test`.

Convention plugins: `mega.android.library`, `mega.android.library.compose`, `mega.android.hilt`, plus the Compose screenshot plugin.

## Testing
JUnit 5 + Mockito + Turbine + Truth, per the root conventions. Tests live in `src/test/...` (mapper + component tests) and screenshot tests in `src/screenshotTest/...` with references under `src/screenshotTestDebug/reference/` (light/dark themes).

- Unit tests: `./gradlew shared:contact:testDebugUnitTest`
- Screenshot tests: `./gradlew shared:contact:validateDebugScreenshotTest` (update references with `updateDebugScreenshotTest`).

## Notes & Gotchas
- Keep this module presentation-only: no `ViewModel`s, navigation, `Context`, or string-resource resolution in mappers — callers resolve status/subtitle strings and pass them in.
- Don't confuse with `:feature:contact:contact` (the full feature) or `:feature:contact:contact-snowflake-components`; put reusable, feature-agnostic pieces here.
- Add or update a screenshot reference when changing any `components/` UI, or `validateDebugScreenshotTest` will fail.
- `AvatarData.Initials` renders only the first character of `initials`.
