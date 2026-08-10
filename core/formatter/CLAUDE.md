# CLAUDE.md

This file provides guidance to Claude Code when working in the `:core:formatter` module.

> Module path: `:core:formatter` · Build file: `core/formatter/formatter.gradle.kts` · Namespace: `mega.privacy.android.core.formatter`

## Overview
`:core:formatter` is a small, shared utility module that converts domain values into user-facing, localized display strings (file/storage sizes, modification dates, currency prices, durations) and provides MEGA link parsing/annotation helpers. It depends only on `:domain` and `:resources:string-resources`, so it can be consumed broadly by feature and presentation modules without pulling in heavy dependencies.

## Architecture & Layout
Source root: `src/main/java/mega/privacy/android/core/formatter/`

- Root package — top-level formatter functions and helper objects (`DateFormatter.kt`, `FileSizeFormatter.kt`, `LinkFormatter.kt`, `StringAnnotationFormatter.kt`).
- `mapper/` — Hilt-injectable mappers (`@Inject` constructor + `operator fun invoke`) following project mapper conventions.
- `model/` — simple data holders returned by mappers (`FormattedSize`).

## Key Components
- `formatModifiedDate(locale, modificationTime)` (`DateFormatter.kt`) — formats a Unix-seconds timestamp into a locale-aware "d MMM yyyy HH:mm" string.
- `formatFileSize(size, context)` (`FileSizeFormatter.kt`) — formats a byte count (B/KB/MB/GB/TB/PB/EB) mimicking iOS `ByteCountFormatter` `.memory` style; KB rounded to whole, MB 1 decimal, GB+ 2 decimals. Uses 1024-based units and `SharedR.string.label_file_size_*`.
- `LinkFormatter` (object) — `extractLinkWithoutKey()` / `extractDecryptionKey()` split a MEGA link from its decryption key, handling both old (`#!`, `#F!`) and new (`#`) link formats.
- `String.stripLinkAnnotations()` (`StringAnnotationFormatter.kt`) — strips MEGA `[A]/[B]/[C]` markup tags to yield plain text.
- `DurationInSecondsTextMapper` (`mapper/`) — maps a `kotlin.time.Duration` to "H:MM:SS" or "M:SS" text.
- `FormattedPriceMapper` (`mapper/`) — maps a domain `CurrencyAmount` to a locale-formatted currency string.
- `FormattedSizeMapper` (`mapper/`) — maps an Int (GB) to a `FormattedSize` (unit string-res id + formatted value), choosing GB vs TB and placeholder vs standalone resources.
- `FormattedSize` (`model/`) — data class holding a unit string-resource id and the formatted size string.

## Module Dependencies
- `:domain` — provides `CurrencyAmount` and related domain entities.
- `:resources:string-resources` — provides `mega.privacy.android.shared.resources.R` size/unit string templates.
- Convention plugins: `mega.android.library`, `mega.android.hilt`.
- Test: `:core-test`, `:core-ui-test`, JUnit5 BOM + bundles.

## Testing
JUnit5 + Truth. Run: `./gradlew core:formatter:testDebugUnitTest`

Currently only `DurationInSecondsTextMapperTest` exists under `src/test`; add tests alongside new formatters/mappers.

## Notes & Gotchas
- `formatModifiedDate` expects time in **seconds** (it multiplies by 1000 internally) — do not pass milliseconds.
- File size formatting is binary (1024-based), not SI (1000-based), and intentionally matches iOS behavior for cross-platform consistency.
- `formatFileSize` needs a `Context` (resolves string resources); the mappers do not and are Hilt-injectable, preferred for testability.
- `LinkFormatter` returns `null` for unrecognized link formats — callers must handle the null case.
