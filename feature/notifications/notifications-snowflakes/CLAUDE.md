# CLAUDE.md

This file provides guidance to Claude Code when working in the `:feature:notifications:notifications-snowflakes` module.

> Module path: `:feature:notifications:notifications-snowflakes` · Build file: `feature/notifications/notifications-snowflakes/notifications-snowflakes.gradle.kts` · Namespace: `mega.privacy.android.feature.notifications.snowflakes`

## Overview
A "snowflake" library of small, reusable Jetpack Compose UI components for the notifications feature. It contains presentation-only composables with no ViewModels, use cases, or data layer — consumers pass in already-resolved state and click callbacks.

## Architecture & Layout
Single flat package `mega.privacy.android.feature.notifications.snowflakes` holding stateless composables. No domain/data layers.

## Key Components
- **UI components**: `NotificationItemViewM3` — Material 3 notification list row rendering a type/section title, title with optional "new" chip for unread items, optional spanned description, optional sub-text, date, and a subtle/strong divider depending on read state. Uses core-ui (`MegaText`, `SpannedText`, `MegaChip`, dividers) and DS tokens. Exposes public test tags (e.g. `NOTIFICATION_ITEM_VIEW_M3_TEST_TAG`).

## Module Dependencies
- `:resources:icon-pack`, `:resources:string-resources`
- core-ui (`lib.mega.core.ui`) and DS tokens (`lib.mega.core.ui.tokens`)
- Compose BOM, Material 3 (incl. adaptive + window size), Coil 3

## Testing
JUnit 5 + Compose UI tests (`:core-test`, `:core-ui-test`). Run: `./gradlew feature:notifications:notifications-snowflakes:testDebugUnitTest`

## Notes & Gotchas
- "Snowflake" = a standalone, dependency-light reusable component module; there is no parent notifications feature module to depend on.
- Keep components stateless and presentation-only — do not add ViewModels, use cases, or repository/data dependencies here.
- Spanned text uses `[A]…[/A]` / `[B]…[/B]` indicators mapped to primary/secondary text colors via the internal `spanStyles` map.
- See root `.claude/CLAUDE.md` for global Compose/Material 3 and 4-space formatting rules.
