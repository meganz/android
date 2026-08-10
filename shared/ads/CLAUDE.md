# CLAUDE.md

This file provides guidance to Claude Code when working in the `:shared:ads` module.

> Module path: `:shared:ads` · Build file: `shared/ads/ads.gradle.kts` · Namespace: `mega.privacy.android.shared.ads`

## Overview
`:shared:ads` holds the shared in-app advertising logic and UI reused across feature modules. It wraps the Google Mobile Ads SDK (`com.google.android.libraries.ads.mobile.sdk`) plus the User Messaging Platform (UMP) consent flow, and exposes Compose entry points for showing banner ads, rewarded ads, and an "ads-free" upsell that points users toward upgrading to a Pro plan.

Ads visibility is gated by feature flags (`ApiFeatures`), Google consent state, and per-screen rules (e.g. file/folder links created by Pro users show no ad). Ad unit IDs are injected per build type via `BuildConfig` (`AD_UNIT_ID`, `REWARDED_AD_UNIT_ID`) — debug uses Google test IDs, release uses production IDs.

## Architecture & Layout
- `ads` (root package): top-level banner containers — `AdsContainer`, `NewAdsContainer`, `AdsContainerViewModel`.
- `ads.advertisements`: banner ad orchestration — `GoogleAdsManager`, `AdsViewModel`, `AdsUiState`.
- `ads.rewarded`: rewarded-ad gating flow — gate handler, dialog, ViewModel, UI state.
- `ads.adsfreeintro`: "ads-free" upsell intro screen — view, ViewModel, UI state, list item.
- `res/`: drawables (ad-free icon, outline icons) used by the upsell UI.

## Key Components
- **Banner containers**: `NewAdsContainer` (current Compose entry point, renders a banner below `content`, gated by `showAdsForScreen`), `AdsContainer` (`AndroidView` wrapping the SDK `AdView`/`BannerAd`), `AdsContainerViewModel`.
- **Banner orchestration**: `GoogleAdsManager` (`@ActivityScoped`; manages UMP `ConsentInformation`, builds `BannerAdRequest`, handles ad refresh and feature-flag/cookie checks), `AdsViewModel` (`@HiltViewModel`; drives `NewAdsContainer`, monitors consent + user data, schedules refresh), `AdsUiState`.
- **Rewarded ads**: `RewardedAdGateHandler` / `rememberRewardedAdGate` (holds the pending action in Compose scope to survive config changes), `RewardedAdDialog`, `RewardedAdGateViewModel` (attempt-count tracking + `AD_SHOW_THRESHOLD`), `RewardedAdGateUiState`.
- **Ads-free upsell**: `AdsFreeIntroView`, `AdsFreeIntroViewModel`, `AdsFreeIntroUiState`, `AdsFreeItem` (benefit row). Navigates to `UpgradeAccountNavKey` / `AdsFreeIntroNavKey`.

## Module Dependencies
- Project modules: `:domain`, `:data`, `:navigation`, `:core:feature-flags`, `:core:formatter`, `:core:analytics:analytics-tracker`, `:core:ui-components:shared-components`, `:resources:string-resources`, `:resources:icon-pack`, `:lint`.
- Notable external libs: Google Mobile Ads SDK (`google.ads.mobile.sdk`) + UMP consent, MEGA core-ui (`lib.mega.core.ui`, tokens), Compose BOM + Material3, Navigation3 runtime, Hilt navigation, DataStore preferences, Coil3, MEGA analytics, Timber, Gson, androidx.webkit.
- The build explicitly excludes the legacy `play-services-ads` / `play-services-ads-lite` artifacts so they are not pulled in transitively.

## Testing
JUnit5 + Mockito + Turbine + Truth. ViewModel tests live under `src/test` (`AdsFreeIntroViewModelTest`, `RewardedAdGateViewModelTest`). Run: `./gradlew shared:ads:testDebugUnitTest`.

## Notes & Gotchas
- Uses the newer `com.google.android.libraries.ads.mobile.sdk` SDK, not the legacy `play-services-ads`; do not reintroduce the excluded artifacts.
- Ad gating is multi-layered: feature flag (`ApiFeatures`) + Google consent (`MonitorGoogleConsentLoadedUseCase`) + per-screen `showAdsForScreen`. Honor all three when adding new ad surfaces.
- `GoogleAdsManager` is `@ActivityScoped` and lifecycle-aware; banner refresh respects `MINIMUM_AD_REFRESH_INTERVAL`.
- The rewarded-ad pending action is intentionally held in Compose scope (`RewardedAdGateHandler`), not the ViewModel, to avoid stale references across config changes.
- Ad unit IDs come from `BuildConfig`; never hardcode unit IDs in source.
