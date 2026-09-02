# CLAUDE.md

This file provides guidance to Claude Code when working in the `:feature:myaccount:myaccount-snowflake-components` module.

> Module path: `:feature:myaccount:myaccount-snowflake-components` · Build file: `feature/myaccount/myaccount-snowflake-components/myaccount-snowflake-components.gradle.kts` · Namespace: `mega.privacy.android.feature.myaccount.snowflakecomponents`

## Overview
A "snowflake" library of reusable Jetpack Compose UI for the My Account achievements / free-trial area. It is presentation-only — stateless composables that take already-resolved state and styling flags; it owns no ViewModels, use cases, or data layer. Consumers (e.g. account / achievements screens) drop these components into their own screens.

It currently contains a single component, `FreeTrailAchievementAwardedText`.

## Architecture & Layout
Single flat package `mega.privacy.android.feature.myaccount.snowflakecomponents` under `src/main/java/`, holding stateless composables. No domain/data layers.

## Key Components
- **`FreeTrailAchievementAwardedText`** — renders a centered text badge for a free-trial achievement. Background and border styling vary by `isPermanent` (transparent, no border), `isReceivedAward`, and `isExpired` (warning border when an expired award was received), using `DSTokens` colors and `MegaText` (`TextColor.Secondary`).

## Module Dependencies
- Project: `:resources:icon-pack`, `:resources:string-resources`, `:domain`.
- External: MEGA core-ui (`lib.mega.core.ui`) + DS tokens (`lib.mega.core.ui.tokens`), Compose BOM + Material 3 (incl. adaptive + window), Coil 3, Timber.
- Convention plugins: `mega.android.library`, `mega.android.library.compose`, `mega.android.hilt`, plus the Kotlin serialization plugin. Unit tests run on `targetSdk = 34`.

## Testing
JUnit 5 + Compose UI tests (`:core-test`, `:core-ui-test`). Existing coverage: `FreeTrailAchievementAwardedTextTest`. Run:
`./gradlew :feature:myaccount:myaccount-snowflake-components:testDebugUnitTest`

## Notes & Gotchas
- **Snowflake module — `DSTokens` are permitted here.** Keep components stateless and presentation-only (no ViewModels, use cases, or repository/data dependencies).
- **Pre-existing misspelling**: the component name is `FreeTrailAchievementAwardedText` — "Trail" is a typo for "Trial". Match the existing spelling when referencing it; do not silently rename without updating all usages.
- `isPermanent` renders the badge with a transparent background and no border — preserve that branch when adding styling states.
- See root `.claude/CLAUDE.md` for global Compose / Material 3 and 4-space formatting conventions.
