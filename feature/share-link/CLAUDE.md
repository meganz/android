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
- `session/` — `ShareLinkSession`, the flow-scoped state holder (password, separate-key choice, resolved link).

NavKeys (`ShareLinkNavKey`, `LinkSettingsNavKey`) live in `:navigation` (`destination/ShareLinkDestinations.kt`), not here.

## Navigation seam (flag gating)
The `{GetLink,ManageLink}ActionClickHandler` (currently in deprecated `:core:ui-components:node-components`) navigate to `ShareLinkNavKey` **unconditionally** — click handlers are synchronous and cannot read the suspend flag, so the flag is **not** checked at the call site (this is the repo-wide convention; see `FileLinkRevamp`). The `shareLinkScreen` entry wraps content in `FeatureFlagGate(ApiFeatures.ShareLinkRevamp)`; when the flag is **off** (default) it removes itself and redirects to the legacy `GetLinkNavKey` → `GetLinkActivity`.

## Testing
JUnit 5 + Mockito + Turbine + Truth for ViewModels; Compose UI tests per screen. Run: `./gradlew feature:share-link:testDebugUnitTest`.

## Flow-scoped session
Link passwords, the "separate link and key" choice and the resolved link are client-side state the SDK cannot report back, so both screens read them from a shared `ShareLinkSession`. It is owned by `ShareLinkSessionViewModel` in the `share_link_flow` shared ViewModel scope: the `ShareLinkNavKey` entry declares `provideSharedViewModelScope`, `LinkSettingsNavKey` joins with `withSharedViewModelStoreKey`, and popping the Share link entry discards the session.

The lifetime is load-bearing, not incidental. This state used to be held process-wide and keyed by node handle, and a handle outlives its link, so anything held past the flow re-attached to a link recreated for the same node (AND-24697). `ShareLinkSession` is deliberately not injectable — the shared scope is the only way to reach one.

**One session serves one subject**, so the state is held directly rather than in a map: the flow opens for a single album or node selection (of which only the first is editable, `ShareLinkSubject.cacheKey`), and it is a leaf — nothing reachable from it opens another Share link screen. A second entry point on top of itself would break that assumption.

## Notes & Gotchas
- New components only where the design system lacks them: `ShareLinkDetailRow`, `ShareLinkDetails`, `MegaDatePickerDialog`. Everything else maps to existing `mega.android.core.ui` components — never hardcode hex; use `DSTokens`.
- Multi-node share and the legacy-cleanup (delete `app/getLink/*`, remove the flag) are later MRs (AND-24043 / AND-24046).
