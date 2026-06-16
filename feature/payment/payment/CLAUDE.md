# CLAUDE.md

This file provides guidance to Claude Code when working in the `:feature:payment:payment` module.

> Module path: `:feature:payment:payment` · Build file: `feature/payment/payment/payment.gradle.kts` · Namespace: `mega.privacy.android.feature.payment`

## Overview
This module owns the in-app upgrade / subscription experience: presenting Pro plans, launching the Google Play billing purchase flow, showing account storage info, and guiding users through plan cancellation (with a cancellation survey and platform-specific instructions for Play, Apple, and Web). It integrates Google Play Billing (`billing.client.ktx`) and exposes its upgrade screen to the app via the Navigation3 `FeatureDestination` contract.

It follows the standard Clean Architecture split (presentation / domain / data). Most business logic lives in the shared `:domain` and `:data` modules; this module holds the presentation layer plus a few payment-specific use cases and mappers. See root `.claude/CLAUDE.md` for global conventions.

## Architecture & Layout
- `presentation/upgrade/` — main upgrade account flow (screen, route, activity, ViewModel, preview/content composables).
- `presentation/billing/` — `BillingViewModel` driving the Play Billing purchase flow.
- `presentation/storage/` — `AccountStorageViewModel` and storage UI state.
- `presentation/cancelaccountplan/` — cancel-plan flow: ViewModel, activity, survey view, `view/` composables (tables, instruction steps) and `view/instructionscreens/` (Web/Apple/Play cancellation instructions).
- `usecase/`, `domain/` — payment-specific use cases.
- `model/` — UI state, UI models, plus `model/mapper/` and `model/extensions/`.
- `navigation/`, `di/` — Navigation3 destination wiring.

## Key Components
- **ViewModels**: `UpgradeAccountViewModel` (Assisted-injected; pricing, subscriptions, account detail), `BillingViewModel` (`@HiltViewModel`; purchase flow + billing events), `AccountStorageViewModel`, `CancelAccountPlanViewModel` (internal), `WebInstructionsViewModel`.
- **Use Cases**: `LaunchPurchaseFlowUseCase` (delegates to `AndroidBillingRepository.launchPurchaseFlow`), `GetBillingCountryCodeUseCase` (internal; `BillingRepository` + `EnvironmentRepository`). Most other use cases (e.g. `GetPricing`, `GetSubscriptionsUseCase`, `MonitorBillingEventUseCase`, `CancelSubscriptionWithSurveyAnswersUseCase`) are consumed from `:domain`.
- **Repositories / Gateways / Data sources**: No repositories defined here — depends on `BillingRepository`/`EnvironmentRepository` interfaces (`:domain`) and `AndroidBillingRepository` (`:data`).
- **Navigation**: `UpgradeAccountNavKey` (from `:navigation`) handled by `upgradeScreens()` (`UpgradeScreenDestination.kt`); `UpgradeFeatureDestination` implements `FeatureDestination` and is multibound `@IntoSet` via `PaymentNavigationModule`. Upgrade entry uses overlay suppression metadata.
- **UI**: `UpgradeAccountScreen` / `UpgradeAccountRoute` / `UpgradeAccountActivity`, `CancelAccountPlanActivity` + `CancelAccountPlanView`, `CancelSubscriptionSurveyView`, `MegaTable*` composables, and the Web/Apple/Play `*InstructionsView` screens.

## Module Dependencies
Internal: `:feature:payment:payment-snowflake-components`, `:core:ui-components:shared-components`, `:core:feature-flags`, `:core:formatter`, `:core:analytics:analytics-tracker`, `:core:navigation-contract`, `:navigation`, `:domain`, `:data`, `:shared:original-core-ui`, `:resources:string-resources`, `:resources:icon-pack`, `:lint`.
External: Google Play Billing (`billing.client.ktx`), Compose BOM + Navigation3 runtime + Material3 adaptive navigation suite, Hilt navigation, kotlinx-serialization, `compose.state.events`, MEGA core-ui / analytics, Timber.

## Snowflake Components
Paired submodule `:feature:payment:payment-snowflake-components` holds reusable Compose UI for the upgrade flow: `ProPlanCard`, `FreePlanCard`, `UpgradeAccountScreenTopBar`, `UpgradeAccountSkeleton`, `BuyPlanBottomBar`, `MaybeLaterNavigationButton`, `NewFeatureRow`, `AdditionalBenefitProPlanView`, `PurchaseSuccessDialog`. Add presentation-agnostic, reusable upgrade widgets here rather than in this module.

## Testing
JUnit 5 + Mockito + Turbine + Truth (test deps via `:core-test` / `:core-ui-test` bundles). Run: `./gradlew feature:payment:payment:testDebugUnitTest`.

## Notes & Gotchas
- `UpgradeAccountViewModel` is Assisted-injected (`@AssistedFactory` / `@AssistedInject`) — instantiate via its factory, not plain `@HiltViewModel` retrieval.
- Billing operations require an `Activity` reference (`LaunchPurchaseFlowUseCase`/`BillingViewModel`), so the flow is bound to `UpgradeAccountActivity` rather than a pure Compose entry point.
- `lint { abortOnError = true }` — lint failures break the build for this module.
- Cancellation instructions branch by payment platform (Play / Apple / Web); keep `CancellationInstructionsTypeMapper` in sync when adding payment methods.
