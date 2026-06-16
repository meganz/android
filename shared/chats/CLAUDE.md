# CLAUDE.md

This file provides guidance to Claude Code when working in the `:shared:chats` module.

> Module path: `:shared:chats` · Build file: `shared/chats/chats.gradle.kts` · Namespace: `mega.privacy.android.shared.chats`

## Overview

Small shared module holding chat-related Jetpack Compose UI building blocks reused across feature modules. Currently it provides the "chat explorer" list row — the selectable row used when picking a chat target (note-to-self, group chat, meeting, 1:1 chat, or contact).

It is a UI-only library (`mega.android.library` + `mega.android.library.compose` convention plugins, `kotlin-parcelize`). It contributes presentation components and their UI models; it does not own use cases or repositories.

## Architecture & Layout

`src/main/java/mega/privacy/android/shared/chats/`
- `components/` — Compose composables (e.g. `ChatExplorerListItemView`).
- `model/` — UI models / state holders (e.g. `ChatExplorerUiItem`).

## Key Components

- `ChatExplorerListItemView` (`components/ChatExplorerListItemView.kt`) — Composable rendering one chat explorer row. A public overload dispatches on the `ChatExplorerUiItem` variant; an `internal` overload draws the shared `GenericListItem` (leading avatar/icon, title, subtitle, trailing selection checkbox). Maps `ChatStatus` to a subtitle string and exposes `testTag` constants for tests.
- `ChatExplorerUiItem` (`model/ChatExplorerUiItem.kt`) — `@Immutable` sealed class for a row. Variants: `NoteToSelf`, `GroupChat`, `Meeting`, `OneToOneChat`, `Contact`, grouped under `GroupChatAndMeeting` and `OneToOneChatAndContact`. Carries id, selection/enabled/archived flags, timestamp, icon, and avatar colors.

## Module Dependencies

Project modules: `:domain`, `:data`, `:navigation`, `:core:feature-flags`, `:core:formatter`, `:resources:string-resources`, `:resources:icon-pack`, `:core:analytics:analytics-tracker`.

Notable external libs: MEGA core-ui and core-ui-tokens (Compose components/theme), Compose BOM + Material3, navigation3-runtime, accompanist-permissions, datastore-preferences, Coil3, Timber, Gson, kotlinx-serialization.

## Testing

JUnit5 + Mockito + Truth, with Compose UI tests (`core-ui-test`) and Hilt test runner. Run:

`./gradlew shared:chats:testDebugUnitTest`

## Notes & Gotchas

- UI models are `@Immutable`; keep new fields immutable so Compose skipping stays valid.
- The `internal` `ChatExplorerListItemView` overload is the rendering core — drive new row types through the public variant-dispatch overload rather than calling it directly.
- Avatar primary/secondary colors are derived per-variant in `ChatExplorerUiItem` (computed getters), not stored for the icon-based variants — change them there, not at call sites.
- Selection is shown via a non-interactive trailing `Checkbox` (`clickable = false`); the whole row handles clicks through `onItemClicked`.
- Use the exported `testTag` constants (e.g. `CHECKBOX_TAG`, `TITLE_TAG`) when writing UI tests rather than hardcoding tag strings.
