# CLAUDE.md

This file provides guidance to Claude Code when working in the `:core:ui-components:achievement-snowflake-components` module.

> Module path: `:core:ui-components:achievement-snowflake-components` · Build file: `core/ui-components/achievement-snowflake-components/achievement-snowflake-components.gradle.kts` · Namespace: `mega.privacy.android.core.achievementcomponents`

> ⚠️ **Deprecated module — do not add new code here.** Achievements should live in their own dedicated feature module (`:feature:achievements`); new achievement UI/logic belongs there, not in this `core` snowflake. When you touch or reuse anything in this module, take the opportunity to migrate it into `:feature:achievements` (and update its callers) rather than extending it here.

## Overview
A tiny "snowflake" library of reusable Jetpack Compose UI for the achievements / free-trial area. It is presentation-only — stateless composables that take already-resolved state and styling flags; it owns no ViewModels, use cases, or data layer. Consumers (e.g. account / achievements screens) drop these components into their own screens.

It currently contains a single component, `FreeTrailAchievementAwardedText`.

## Architecture & Layout
Single flat package `mega.privacy.android.core.achievementcomponents` under `src/main/java/`, holding stateless composables. No domain/data layers.

## Key Components
- **`FreeTrailAchievementAwardedText`** — renders a centered text badge for a free-trial achievement. Background and border styling vary by `isPermanent` (transparent, no border), `isReceivedAward`, and `isExpired` (warning border when an expired award was received), using `DSTokens` colors and `MegaText` (`TextColor.Secondary`).

## Module Dependencies
- Project: `:resources:icon-pack`, `:resources:string-resources`, `:domain`.
- External: MEGA core-ui (`lib.mega.core.ui`) + DS tokens (`lib.mega.core.ui.tokens`), Compose BOM + Material 3 (incl. adaptive + window), Coil 3, Timber.
- Convention plugins: `mega.android.library`, `mega.android.library.compose`, `mega.android.hilt`, plus the Kotlin serialization plugin. Unit tests run on `targetSdk = 34`.

## Testing
JUnit 5 + Compose UI tests (`:core-test`, `:core-ui-test`). Existing coverage: `FreeTrailAchievementAwardedTextTest`. Run:
`./gradlew core:ui-components:achievement-snowflake-components:testDebugUnitTest`

## Notes & Gotchas
- **Deprecated — new achievement work goes in `:feature:achievements`, not here.** Do not add components to this module. If a change forces you to work in here, migrate the affected pieces into a proper `:feature:achievements` module and repoint their usages as part of the change.
- **Pre-existing misspelling**: the component name is `FreeTrailAchievementAwardedText` — "Trail" is a typo for "Trial". Match the existing spelling when referencing it; do not silently rename without updating all usages.
- "Snowflake" = a standalone, dependency-light reusable component module; keep components stateless and presentation-only (no ViewModels, use cases, or repository/data dependencies).
- `isPermanent` renders the badge with a transparent background and no border — preserve that branch when adding styling states.
- See root `.claude/CLAUDE.md` for global Compose / Material 3 and 4-space formatting conventions.
