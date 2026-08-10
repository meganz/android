# CLAUDE.md

This file provides guidance to Claude Code when working in the `:feature:photos:photos` module.

> Module path: `:feature:photos:photos` · Build file: `feature/photos/photos/photos.gradle.kts` · Namespace: `mega.privacy.android.feature.photos`

## Overview
This module implements the MEGA media experience: the Timeline, Albums (system and user albums, sharing/import links, cover/photo selection), Videos and Video Playlists, Camera Uploads progress, media search, and Cloud Drive media discovery. It is a Compose-first feature module wired with Hilt and Navigation3.

It depends on the global app `:domain` for business logic and use cases; only one feature-local use case (`GetNodeListByIds`) lives here. Most logic sits in ViewModels backed by domain use cases, with feature-local mappers, data providers, and caches bridging domain models to UI state.

## Architecture & Layout
Single `presentation`-heavy layer (no full data layer in this module). Top-level packages under `mega.privacy.android.feature.photos`:
- `presentation/` — ViewModels, Screens, and Compose components, organized by sub-feature: `timeline/`, `albums/` (with `content`, `coverselection`, `photosselection`, `getlink`, `getmultiplelinks`, `importlink`, `decryptionkey`, `copyright`, `dialog`), `videos/`, `playlists/` (with `detail`, `videoselect`), `mediadiscovery/`, `search/`, `cuprogress/`, `component/`, `effects/`, `handler/`
- `navigation/` — `MediaFeatureDestination`, route/entry wiring, nav items
- `mapper/` — UI/domain state mappers
- `provider/` — albums data providers and an in-memory photos cache
- `di/` — Hilt modules
- `domain/usecase/`, `model/`, `downloader/`, `extensions/`, `res/` (drawables)

## Key Components
- **ViewModels**: `TimelineTabViewModel`, `AlbumsTabViewModel`, `AlbumContentViewModel`, `AlbumCoverSelectionViewModel`, `AlbumPhotosSelectionViewModel`, `AlbumGetLinkViewModel`, `AlbumGetMultipleLinksViewModel`, `AlbumImportViewModel`, `VideosTabViewModel`, `VideoPlaylistsTabViewModel`, `VideoPlaylistDetailViewModel`, `SelectVideosForPlaylistViewModel`, `SelectVideosSearchViewModel`, `VideoRecentlyWatchedViewModel`, `CameraUploadsProgressViewModel`, `CloudDriveMediaDiscoveryViewModel`, `PhotosSearchViewModel`, `MediaMainViewModel`, `MediaCameraUploadViewModel`, `PhotoDownloaderViewModel`
- **Use Cases**: `GetNodeListByIds` (feature-local; most use cases come from `:domain`)
- **Repositories / Gateways / Data sources**: no repositories here. Data access via providers/caches: `AlbumsDataProvider` (interface) with `SystemAlbumsDataProvider` / `UserAlbumsDataProvider` implementations, and `PhotosCache`
- **Navigation**: `MediaFeatureDestination : FeatureDestination`; Navigation3 `EntryProviderScope<NavKey>` route extensions in `navigation/` (NavKeys such as `MediaMainNavKey`, `AlbumContentNavKey`, `VideoPlaylistDetailNavKey`, `MediaSearchNavKey` are defined in `:navigation`); plus `MediaScreenDestination` and `MediaNavItem`
- **UI**: stateless `*Screen` composables, e.g. `MediaMainScreen`, `TimelineTabScreen`, `AlbumsTabScreen`, `AlbumContentScreen`, `VideosTabScreen`, `VideoPlaylistsScreen`, `VideoPlaylistDetailScreen`, `CameraUploadsProgressScreen`, `CloudDriveMediaDiscoveryScreen`, `SelectVideosForPlaylistScreen`

## Module Dependencies
Project deps: `:domain`, `:navigation`, `:shared:nodes`, `:shared:transfers`, `:core:feature-flags`, `:core:ui-components:shared-components`, `:core:ui-components:node-components`, `:core:analytics:analytics-tracker`, `:core:navigation-contract`, `:core:coroutine`, `:core:formatter`, `:core:transfers`, `:resources:icon-pack`, `:resources:string-resources`, `:feature:photos:photos-snowflake-components`, `:feature:transfers:transfers-snowflake-components`.

Notable external libs: MEGA core-ui, MEGA analytics, Compose BOM + activity/viewmodel, Material3 adaptive navigation suite, Navigation3 (runtime + ui), Hilt navigation, kotlinx-serialization, kotlinx-collections-immutable, Coil, compose-state-events, ML Kit document scanner, Timber. Uses the prebuilt MEGA SDK.

## Snowflake Components
Paired submodule `:feature:photos:photos-snowflake-components` holds reusable Compose UI under `…feature/photos/components/`: `AlbumGridItem`, `CameraUploadsStatusIcon`, `EditVideoPlaylistDialog`, `FilterOptionWithRadioButton`, `PhotosNode`, `SelectVideoGridItem`, `SelectVideoListItem`, `ThumbnailListView`, `TimelineFilterViewContent`, `TimelineGridSizeSettingsMenu`, `VideoItemView`, `VideoPlaylistDetailHeaderView`, `VideoPlaylistItemView`, `VideosFilterButtonView`. Add shared/reusable photo & video composables there rather than in this module. Do not create a separate CLAUDE.md for that submodule.

## Testing
JUnit 5 + Mockito + Turbine + Truth (Hilt test support available). Run:
`./gradlew feature:photos:photos:testDebugUnitTest`

## Notes & Gotchas
- Source root is `src/main/java` (not `kotlin`); the snowflake submodule uses `src/main/kotlin`.
- Navigation uses Navigation3 — NavKeys are declared in `:navigation` and bound to entries here via `EntryProviderScope` route extensions; register new destinations through `MediaFeatureDestination`.
- Lint check `CoroutineCreationDuringComposition` is disabled for this module; `abortOnError` is on.
- "Legacy" mappers/keys (e.g. `LegacyMediaSystemAlbumMapper`, `LegacyImageViewerNavKey`) bridge to older screens — check whether a legacy path is intended before reusing.
- Follow global conventions in root `.claude/CLAUDE.md` (4-space indent, ViewModel/UseCase/Mapper skills).
