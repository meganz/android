# CLAUDE.md

This file provides guidance to Claude Code when working in the `:core:feature-flags` module.

> Module path: `:core:feature-flags` · Build file: `core/feature-flags/feature-flags.gradle.kts` · Namespace: `mega.privacy.android.feature_flags`

## Overview
This module hosts the app-wide catalog of feature flags and A/B test flags. Flags are declared as enum entries, each providing a description and a compile-time default value. Each enum's companion object implements `FeatureFlagValueProvider` (from the `:domain` module), so the enum itself acts as a source of default flag values that the wider feature-toggle system reads.

Flags defined here are consumed across the app via the domain feature-toggle abstractions (`Feature`, `FeatureFlagValueProvider`). Runtime overrides (e.g. QA Settings toggles, remote/AB-test values) come from higher-priority providers; entries in this module supply the `Default` priority value.

## Architecture & Layout
Single package `mega.privacy.android.feature_flags` under `src/main/java/`:
- `AppFeatures.kt` — local app feature flags.
- `ABTestFeatures.kt` — remote A/B test flags.

There is no data/domain/presentation split; the module only declares flag enums and depends on domain abstractions.

## Key Components
- `AppFeatures` — `enum class` implementing `mega.privacy.android.domain.entity.Feature`. Each entry has a `description` and a private `defaultValue: Boolean`. Its `companion object : FeatureFlagValueProvider` returns the matching entry's `defaultValue` from `isEnabled(feature)` at `FeatureFlagValuePriority.Default`.
- `ABTestFeatures` — `enum class` implementing `mega.privacy.android.domain.entity.featureflag.ABTestFeature`. Adds `experimentName` (the API AB-test name without the `ab_` prefix) and `checkRemote` (when true the value is fetched from the remote server; when false it can be toggled like a normal flag in QA builds). Companion object likewise implements `FeatureFlagValueProvider` at `Default` priority.

## Module Dependencies
- `:domain` — provides `Feature`, `ABTestFeature`, `FeatureFlagValueProvider`, and `FeatureFlagValuePriority`.

## Testing
JUnit5 + Truth. Run: `./gradlew core:feature-flags:testDebugUnitTest`

## Notes & Gotchas
- Register a new `AppFeatures` flag at the top of the enum list to minimize git-diff churn (per the in-file note).
- A new A/B test campaign requires a new flag on the API side and a new `ABTestFeatures` entry; `devtest` is a non-campaign flag reserved for testing AB-test SDK plumbing.
- Enum entries use PascalCase per project conventions (note `ABTestFeatures` entries use the lowercase experiment name, e.g. `ande`, `devtest`, matching the API name).
- This module only supplies `Default`-priority defaults; the resolved flag value at runtime depends on other providers registered with higher `FeatureFlagValuePriority`.
