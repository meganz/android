# CLAUDE.md

This file provides guidance to Claude Code when working in the `:feature:share-link` module.

> Module path: `:feature:share-link` · Build file: `feature/share-link/share-link.gradle.kts` · Namespace: `mega.privacy.android.feature.sharelink`

## Overview
Revamped **Share & Manage link** feature (DSN-2987 / epic AND-23311) — the Jetpack Compose, new-design-system (`mega.android.core.ui` / `DSTokens`) replacement for the legacy XML `app/getLink/*` flow. Presentation-only: business logic comes from existing `:domain` use cases (export/disable link, password encryption, password strength, account type). Gated behind the `ShareLinkRevamp` flag in `ApiFeatures` (`:domain` `featuretoggle`), mirroring `FileLinkRevamp`.

Two primary screens: **Share link** (result/detail) and **Link settings** (toggle editor). Full plan, MR breakdown and Figma index live in `GetLink_UI_Revamp_Plan.md` at the repo root.

## Architecture & Layout
Single tree under `mega.privacy.android.feature.sharelink`:
- `presentation/` — `*Screen` composables, `*ScreenDestination` entry-providers, `*ViewModel` (assisted-injected, mirroring `FileLinkViewModel`), `*UiState`.
- `navigation/` — `ShareLinkFeatureGraph` (implements `FeatureDestination`).
- `di/` — `ShareLinkModule` contributes the destination `@IntoSet`.

NavKeys (`ShareLinkNavKey`, `LinkSettingsNavKey`) live in `:navigation` (`destination/ShareLinkDestinations.kt`), not here.

## Navigation seam (flag gating)
The `{GetLink,ManageLink}ActionClickHandler` (currently in deprecated `:core:ui-components:node-components`) navigate to `ShareLinkNavKey` **unconditionally** — click handlers are synchronous and cannot read the suspend flag, so the flag is **not** checked at the call site (this is the repo-wide convention; see `FileLinkRevamp`). The `shareLinkScreen` entry wraps content in `FeatureFlagGate(ApiFeatures.ShareLinkRevamp)`; when the flag is **off** (default) it removes itself and redirects to the legacy `GetLinkNavKey` → `GetLinkActivity`.

## Testing
JUnit 5 + Mockito + Turbine + Truth for ViewModels; Compose UI tests per screen. Run: `./gradlew feature:share-link:testDebugUnitTest`.

## Notes & Gotchas
- New components only where the design system lacks them: `ShareLinkDetailRow`, `ShareLinkDetails`, `MegaDatePickerDialog`. Everything else maps to existing `mega.android.core.ui` components — never hardcode hex; use `DSTokens`.
- Multi-node share and the legacy-cleanup (delete `app/getLink/*`, remove the flag) are later MRs (AND-24043 / AND-24046).
