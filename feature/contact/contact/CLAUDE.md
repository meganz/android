# CLAUDE.md

This file provides guidance to Claude Code when working in the `:feature:contact:contact` module.

> Module path: `:feature:contact:contact` · Build file: `feature/contact/contact/contact.gradle.kts` · Namespace: `mega.privacy.android.feature.contact`

## Overview
This module provides the Compose-based contact UI for the MEGA Android app: the contact list, the add-contact (contact selection) flow, and the contact groups screen. It is a presentation-only feature module — it consumes domain use cases (from `:domain`) and renders screens; it contains no repository or data-layer implementations of its own.

Screens are wired into the app via Navigation3 `FeatureDestination` / `AppDialogDestinations` contributions (multibound into Hilt sets), so the app module can host these destinations without a direct compile dependency on the feature.

## Architecture & Layout
Standard MEGA presentation layering, organized by feature area under `mega.privacy.android.feature.contact`:
- `add/` — add/select contacts flow: `AddContactViewModel`, `model/AddContactUiState`, `view/AddContactsScreen`, `view/ContactSelectionState`.
- `list/` — contact list: `ContactListViewModel`, `model/ContactListUiState`, `view/ContactListScreen`, `ContactListContent`, `ContactActionsBottomSheet`.
- `group/` — contact groups: `ContactGroupsViewModel`, `model/ContactGroupUiState`, `model/ContactGroupItem`, `mapper/ContactGroupItemMapper`, `view/ContactGroupsScreen` (+ content/loading views), `navigation/ContactGroupsDestination`.
- `navigation/` — feature/destination wiring (`ContactFeatureDestination`, `ContactListDestination`, `AddContactsDestination`, `CannotVerifyContactDialogM3Navigation`).
- `dialog/` — `CannotVerifyContactDialogM3`.
- `components/` — shared UI pieces (e.g. `ContactListLoadingView`).
- `di/` — `ContactModule`.

## Key Components
- **ViewModels**: `AddContactViewModel`, `ContactListViewModel`, `ContactGroupsViewModel`.
- **Use Cases** (injected from `:domain`): `GetContactsUseCase`, `RemoveContactByEmailUseCase`, `Get1On1ChatIdUseCase`, `StartCallUseCase`, `GetChatCallUseCase`, `MonitorContactRequestsUseCase`, `CreateGroupChatRoomUseCase`, `GetContactGroupsUseCase`.
- **Repositories / Gateways / Data sources**: None — this module has no data layer.
- **Mappers**: `ContactGroupItemMapper` (domain group → `ContactGroupItem` UI model).
- **Navigation**: `ContactFeatureDestination` (implements `FeatureDestination`) and `ContactFeatureDialogDestinations` (implements `AppDialogDestinations`), both multibound via `ContactModule` `@IntoSet`. Local nav key: `ContactGroupsNavKey` (`internal data object`). Cross-feature nav keys (`AddContactsNavKey`, `ContactInfoNavKey`, `ContactRequestsNavKey`, `InviteContactNavKey`, `CreateGroupChatNavKey`, `ShowChatMessagesNavKey`, `LegacyMeetingNavKey`) come from `:navigation` (`mega.privacy.android.navigation.destination`); results are passed back via `NavigationHandler.monitorResult`/`clearResult`.
- **UI**: Jetpack Compose (Material3 + adaptive navigation-suite), Coil for avatars, `kotlinx.collections.immutable` for stable list state, compose-state-events for one-shot UI events.

## Module Dependencies
Project deps: `:domain`, `:navigation`, `:shared:contact`, `:core:feature-flags`, `:core:analytics:analytics-tracker`, `:core:navigation-contract`, `:core:coroutine`, `:core:ui-components:node-components`, `:resources:icon-pack`, `:resources:string-resources`, plus the prebuilt MEGA SDK. Lint: `:lint` + Slack compose-lints.

Notable external libs: `lib.mega.core.ui`, `lib.mega.analytics`, Navigation3 runtime/ui, Material3 adaptive navigation-suite, Hilt navigation, Coil Compose, kotlinx-serialization, ML Kit document scanner, Timber.

Convention plugins: `mega.android.library`, `mega.android.library.compose`, `mega.android.hilt`; plus kotlin-serialization and the compose screenshot plugin (`android.experimental.enableScreenshotTest` enabled).

## Snowflake Components
Paired submodule `:feature:contact:contact-snowflake-components` (namespace `mega.privacy.android.feature.contact.components`) currently holds **no production composables** — its reusable contact UI (notably `ContactItemView`) has been migrated to `:shared:contact`. Only a screenshot test (`ShareContactOptionsContactItemScreenshotTest`, with light/dark reference images under `src/screenshotTestDebug/reference/`) remains, validating the migrated `ContactItemView` row. Put new reusable, feature-agnostic contact UI in `:shared:contact`, not here. Do not add a separate CLAUDE.md for this submodule.

## Testing
JUnit 5 + Mockito + Turbine + Truth (per root `.claude/CLAUDE.md` conventions). ViewModel, mapper, and Compose screen tests live in `src/test`; screenshot tests (`compose.screenshot`) live in `src/screenshotTest`.
Run: `./gradlew feature:contact:contact:testDebugUnitTest`

## Notes & Gotchas
- Presentation-only: add new business logic as use cases in `:domain`, not here.
- Cross-feature navigation goes through `NavigationHandler` using nav keys defined in `:navigation`; only screens owned by this feature (e.g. `ContactGroupsNavKey`) define keys locally, and those are `internal`.
- The contacts Compose UI is feature-flag gated (`ContactsComposeUI`) and hosted by the app module — destinations here are contributed, not self-registered.
- Inter-screen results (e.g. created group chat from `CreateGroupChatNavKey`) are delivered via `monitorResult`; remember to `clearResult` after consuming to avoid re-processing.
