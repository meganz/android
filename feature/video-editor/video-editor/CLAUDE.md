# CLAUDE.md

This file provides guidance to Claude Code when working in the `:feature:video-editor:video-editor` module.

> Module path: `:feature:video-editor:video-editor` · Build file: `feature/video-editor/video-editor/video-editor.gradle.kts` · Namespace: `mega.privacy.android.feature.videoeditor`

## Overview
On-device video editing feature. It loads a source video (by `nodeHandle`, resolved to a local file), previews it with Media3/ExoPlayer, lets the user apply non-destructive tools (trim, crop, rotate, speed, volume), and exports the result with Media3 `Transformer`, reporting the export back out as a MEGA transfer event.

The module is split into a thin MEGA-aware host (`VideoEditorScreenViewModel` — node resolution, download, collision handling, transfer events) and a self-contained editing engine (`EditorViewModel` + a pure MVI reducer) that knows nothing about the MEGA SDK. All reusable Compose UI primitives and gesture math live in the paired `video-editor-snowflakes` submodule.

## Architecture & Layout
Standard clean-architecture split under `presentation/`, `domain/`, `data/`:
- `data/` — `VideoEditorRepository` and `VideoMetadataGateway` (framework `MediaMetadataRetriever` wrapper, run on `@IoDispatcher`).
- `domain/` — `VideoMetadata` entity, `GetVideoMetadataUseCase`.
- `presentation/screen/` — host route, `VideoEditorScreenViewModel`, `VideoEditorScreenDestination` (NavKey entry), UI state.
- `presentation/editor/` — the engine. Sub-packages: `state/` (immutable `EditorState`, `EditorAction`, `PlaybackState`, `SourceState`), `engine/` (pure `reduce`, `ToolRegistry`, `EffectComposer`, `MediaItemBuilder`, `PreviewPlayer`), `tool/` (one package per tool + `tool/api/` contracts), `render/` (`EditorPreview`, `PreviewGeometry`), `export/` (`VideoExporter`, `ExportProgress`/`ExportEvent`, `MetadataMuxerFactory`), `ui/` (editor-local composables: dialogs, tool deck, preview controls).
- `di/`, `navigation/` — Hilt wiring and the feature graph.

## Key Components
- **ViewModels**: `VideoEditorScreenViewModel` (Hilt + `@AssistedInject`; MEGA node/transfer host), `EditorViewModel` (`@HiltViewModel`; drives the MVI `EditorState` via `reduce` against the injected `ToolRegistry`).
- **Use Cases**: `GetVideoMetadataUseCase`.
- **Repositories / Gateways / Data sources**: `VideoEditorRepository`, `VideoMetadataGateway`.
- **Navigation**: `VideoEditorFeatureGraph : FeatureDestination`; `videoEditorScreen` registers `entry<VideoEditorScreenNavKey>` (NavKey defined in `:navigation`); callbacks via `NavigationHandler` / `TransferHandler`.
- **UI**: Compose throughout — `VideoEditorScreen`, `EditorPreview`, `ToolDeck`, `PreviewControls`, `ExportDialog`, `PrepareDialog`, `DiscardChangesDialog`.

## Module Dependencies
Modules: `:feature:video-editor:video-editor-snowflakes`, `:core:formatter`, `:core:navigation-contract`, `:navigation`, `:core:ui-components:shared-components`, `:domain`, `:resources:icon-pack`, `:resources:string-resources`, plus the pre-built SDK.
Notable external libs: full **Media3** bundle (ExoPlayer, Transformer, PlayerSurface — the editor engine + preview + export live here), Compose / Material3, Navigation3, Hilt navigation, `compose-state-events`, Coil3, Timber. Note: `core-ui` is pulled in only for the Compose runtime/foundation — DSTokens/core-ui components are used from the snowflakes module, not here.

## Snowflake Components
Paired submodule `:feature:video-editor:video-editor-snowflakes` (package `mega.privacy.android.feature.videoeditor.components`) holds stateless, reusable editor UI pieces and gesture math: `CropOverlay`, `CropGestureMath`, `Filmstrip`, `ToolTabBar`, `AspectRatioChip`, `SpeedChip`, `RotateTile`, `VolumeSlider`, `BlockingProgressDialog`. Edit these here; do **not** create a separate CLAUDE.md for the submodule.

## Testing
JUnit5 + Mockito + Turbine + Truth (Robolectric available; UI test bundles included). Run:
```
./gradlew feature:video-editor:video-editor:testDebugUnitTest
```

## Notes & Gotchas
- Media3 editor/transformer APIs are `@UnstableApi` — most engine/DI/tool entry points are annotated `@OptIn(UnstableApi::class)`; keep that when extending them.
- Tools are contributed via Hilt `@IntoSet` multibinding into a `Set<EditorTool>` and assembled into the singleton `ToolRegistry`. Add a new tool by providing it `@IntoSet` in `VideoEditorModule`; an empty set renders the editor with no tools.
- `EditorViewModel` is deliberately MEGA-free: source video is handed in via `EditorAction.LoadVideo` and export results are reported back out. Keep node/transfer/SDK concerns in `VideoEditorScreenViewModel`.
- `VideoMetadataGateway` is synchronous/blocking and swallows failures, returning an all-zeros `VideoMetadata` sentinel (treat as "metadata not yet available"); always invoke it off the main thread (the repository handles this via `@IoDispatcher`). It also normalises dimensions for rotation metadata.
- Root `.claude/CLAUDE.md` global rules apply (4-space indent, naming, JUnit5 test conventions).
