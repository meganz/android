# CLAUDE.md

This file provides guidance to Claude Code when working in the `:feature:pdfviewer` module.

> Module path: `:feature:pdfviewer` · Build file: `feature/pdfviewer/pdfviewer.gradle.kts` · Namespace: `mega.privacy.android.feature.pdfviewer`

## Overview
Self-contained feature module that renders and searches PDF documents inside the MEGA Android app. It handles every source a PDF can come from: Cloud Drive / Rubbish Bin / Backups nodes, offline files, chat attachments, file links, folder links, ZIP entries, and external-app intents. Remote (streaming) sources are downloaded to bytes before rendering; local sources are opened directly. The viewer also supports password-protected PDFs, in-document text search with highlighting and match navigation, "continue where left off" progress tracking, and a floating action toolbar wired to the shared node-options pipeline.

Rendering uses a vendored copy of the `com.github.barteksc.pdfviewer` library (Java, `PDFView` and friends, under `src/main/java/com/github/barteksc/pdfviewer`) backed by PdfiumCore. Text search is implemented separately on top of `PdfiumCore` (`com.shockwave.pdfium`) via the module's own `PdfSearchEngine`.

## Architecture & Layout
Compose-first feature module following the project's three-layer conventions. Domain logic lives in `:domain` (use cases injected into the ViewModel); this module owns the presentation layer plus a small data-adjacent search component.

- `presentation/` — `PdfViewerViewModel`, `PdfViewerState`, `PdfViewerScreen`, `PdfViewerScreenDestination` (the `pdfViewerScreen` entry-provider extension), `components/` (top bars, dialogs, indicators, share intents), and `model/` (`PdfViewerSource`, `PdfViewerError`).
- `search/` — `PdfSearchEngine` interface + `PdfSearchEngineImpl` (PdfiumCore), and `PdfSearchEngineFactory` / `DefaultPdfSearchEngineFactory`.
- `navigation/` — `PdfViewerFeatureDestination` (`FeatureDestination` implementation).
- `di/` — `PdfViewerModule` (Hilt bindings).
- `com/github/barteksc/pdfviewer/` — vendored Java PDF rendering library (PDFView, PdfFile, CacheManager, RenderingHandler, sources, listeners, util).

## Key Components
- **ViewModels**: `PdfViewerViewModel` — assisted-injected (`@HiltViewModel(assistedFactory = Factory::class)`) with an `Args` data class supplied by the nav destination. Owns the `PdfSearchEngine`, the debounced search pipeline, rect prefetching, last-viewed-page persistence, and connectivity/offline monitoring. State exposed as `StateFlow<PdfViewerState>`.
- **Use Cases** (from `:domain`, injected): `GetLastPageViewedInPdfUseCase`, `SetOrUpdateLastPageViewedInPdfUseCase`, `GetDataBytesFromUrlUseCase`, `MonitorConnectivityUseCase`, `MonitorOfflineNodeUpdatesUseCase`, `SaveRecentlyUsedItemIfQualifiesUseCase`, `GetFeatureFlagValueUseCase`, `GetNodeByIdUseCase`, `GetPublicNodeUseCase`, `GetPublicNodeByIdUseCase`, `GetOfflineFileInformationByIdUseCase`.
- **Repositories / Gateways / Data sources**: No repository in this module. `PdfSearchEngine` (interface) / `PdfSearchEngineImpl` is the data-source-like component wrapping PdfiumCore; obtained via `PdfSearchEngineFactory` so tests can inject `FakePdfSearchEngine`.
- **Navigation**: `PdfViewerFeatureDestination` (`FeatureDestination`) registers `pdfViewerScreen` against `PdfViewerNavKey` (defined in `:navigation`). Uses Navigation3 (`EntryProviderScope`, `NavKey`). The node-options bottom sheet is registered in the app module, not here.
- **UI**: `PdfViewerScreen` (stateless) plus `components/`: `PdfViewerContent`, `PdfViewerTopBar`, `PdfViewerSearchTopBar`, `PdfSearchResultsBar`, `PdfPageIndicator`, `ExternalFileBottomBar`, `PdfViewerPasswordDialog`, `PdfViewerErrorDialog`, share-intent helpers.

## Module Dependencies
Project modules: `:domain`, `:navigation`, `:core:navigation-contract`, `:core:ui-components:node-components`, `:core:ui-components:shared-components`, `:shared:nodes`, `:core:feature-flags`, `:resources:string-resources`, `:resources:icon-pack`, and `:third-party-lib:pdfiumAndroid` (used directly by the search engine). `:lint` for lint checks.

Notable external libs: MEGA core-ui, Jetpack Compose (BOM + Material3 + activity/viewmodel), Navigation3 (`navigation3.runtime`/`ui`), Hilt + hilt-navigation, kotlinx-serialization, compose-state-events (`EventEffect`), Timber, AppCompat + Material. The prebuilt MEGA SDK is added via `preBuiltSdkDependency`.

## Testing
JUnit 5 + Mockito + Turbine + Truth, with `:core-test` / `:core-ui-test` utilities and Robolectric (see `src/test/resources/robolectric.properties`). The ViewModel is tested with `FakePdfSearchEngine` injected through `PdfSearchEngineFactory`. Existing tests cover the ViewModel, screen, search engine impl, source mapping, and several components.

Run: `./gradlew feature:pdfviewer:testDebugUnitTest`

## Notes & Gotchas
- Two PDF stacks coexist: the vendored Java `barteksc` `PDFView` does the on-screen rendering; the Kotlin `PdfSearchEngine` opens the document a second time (from bytes, URI, or file path) purely for text search. Keep them in sync — e.g. a password change calls `reinitSearchEngine()`.
- `searchEngine` access is guarded by `engineLock` to avoid a race between the init coroutine and `onCleared()`; the engine is closed in `onCleared`. Don't drop this synchronization.
- "Continue where left off" is persisted on exit in `onCleared()` via `applicationScope` (not `viewModelScope`, which is already cancelled). Last-viewed page is persisted independently in `onPageChanged`.
- External files (`isExternalFile`) have no MEGA node (`nodeHandle == -1L`): skip node resolution, page persistence, and continue-where-left-off; back navigation calls `activity.finish()`.
- File-link nodes are resolved from the public link URL (`GetPublicNodeUseCase`), folder-link nodes via `GetPublicNodeByIdUseCase` (account-only lookups would return null). Offline orphans fall back to offline file info mapped via `OfflineTypedNodeMapper`.
- Search is debounced (300ms) and requires query length ≥ 2; rects are prefetched ±`PREFETCH_RADIUS` (1) pages around the current/match page. UI never pushes computed data back up — results and rects flow down through `PdfViewerState`.
- Follow the global conventions in the root `.claude/CLAUDE.md`; this module adds no overrides.
