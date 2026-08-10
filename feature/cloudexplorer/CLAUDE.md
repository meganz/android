# CLAUDE.md

This file provides guidance to Claude Code when working in the `:feature:cloudexplorer` module.

> Module path: `:feature:cloudexplorer` · Build file: `feature/cloudexplorer/cloudexplorer.gradle.kts` · Namespace: `mega.privacy.android.feature.cloudexplorer`

## Overview
This module provides the "explorer" / picker flows used across the app to browse and select MEGA nodes and destinations. It powers actions such as copy, move, import (nodes and albums), select Camera Uploads folder, share files/text to MEGA, share files to chat, add videos to a playlist, and upload scanned documents. Most flows let the user navigate the cloud drive (and related sources like incoming shares and favourites), pick a node/folder, and return the result to the caller via the navigation result mechanism.

It is a presentation-focused feature module: it contains Compose screens and ViewModels and consumes business logic from `:domain` use cases. It does not define its own repositories or gateways.

## Architecture & Layout
Single source root under `src/main/java/mega/privacy/android/feature/cloudexplorer/` with three top-level packages:
- `di/` — Hilt module wiring.
- `navigation/` — `FeatureDestination` implementation and navigation result helpers.
- `presentation/` — Compose UI and ViewModels, organized one sub-package per flow: `addvideotoplaylist`, `chatexplorer`, `components`, `copy`, `explorer`, `favouritesexplorer`, `importalbum`, `importnodes`, `incomingsharesexplorer`, `move`, `nodesexplorer`, `search`, `selectcufolder`, `sharefilestochat`, `sharetomega` (`files` + `text`), `uploadscanneddocument`.

Each flow follows the `*Screen` + `*UiState` + `*ViewModel` triad. NavKeys and result types are not defined here — they live in `:navigation` (`mega.privacy.android.navigation.destination`).

## Key Components
- **ViewModels**: `SelectCUFolderViewModel`, `MoveViewModel`, `CopyViewModel`, `ImportViewModel`, `ImportAlbumViewModel`, `AddVideoToPlaylistViewModel`, `ShareFilesToChatViewModel`, `ShareFilesToMegaViewModel` (assisted `Factory` with `Args`), `ShareTextToMegaViewModel`, `UploadScannedDocumentsViewModel` (assisted `Factory` with `Args`), `NodesExplorerViewModel`, `NodeExplorerSharedViewModel`, `FavouritesExplorerViewModel`, `IncomingSharesExplorerViewModel`, `ChatExplorerViewModel`, `ExplorerSearchViewModel`.
- **Use Cases**: Consumed from `:domain` (none defined here). Notable: `SearchUseCase`, `GetFileBrowserNodeChildrenUseCase`, `GetIncomingSharesChildrenNodeUseCase`, `GetAllFavoritesUseCase`, `GetRootNodeIdUseCase`, `GetNodeByIdUseCase`, `GetAncestorsIdsUseCase`, `GetCopyLatestTargetPathUseCase`, `GetMoveLatestTargetPathUseCase`, `CreateTextFileWithContentUseCase`, `MonitorHiddenNodesEnabledUseCase`, `MonitorShowHiddenItemsUseCase`, `MonitorNodeUpdatesByIdUseCase`, plus chat use cases (`GetActiveChatListItemsUseCase`, `CreateGroupChatRoomUseCase`, `SendTextMessageUseCase`, etc.) and recent-search use cases (`SearchUseCase`, `SaveRecentSearchUseCase`, `ClearRecentSearchesUseCase`, `MonitorRecentSearchesUseCase`).
- **Repositories / Gateways / Data sources**: None in this module. Mapping helper: `ChatExplorerUiItemMapper`.
- **Navigation**: `CloudExplorerFeatureDestination` (implements `FeatureDestination`, contributed `@IntoSet` via `CloudExplorerModule`) registers all `entry<*NavKey>` destinations. `RememberNewFileNameResult` is a navigation result helper. Results flow back through `NavigationHandler` (`returnResult`/`monitorResult`/`clearResult`) and uploads through `TransferHandler`.
- **UI**: Compose `*Screen` composables per flow; shared composables in `presentation/components` (`CloudExplorerViewItems`) and search content composables in `presentation/search` (e.g. `NodesExplorerSearchContent`, `ChatExplorerSearchContent`, `IncomingSharesExplorerSearchContent`, `FavouritesExplorerSearchContent`).

## Module Dependencies
Project modules: `:domain`, `:data`, `:navigation`, `:core:analytics:analytics-tracker`, `:core:navigation-contract`, `:core:coroutine`, `:resources:string-resources`, `:resources:icon-pack`, `:shared:chats`, `:shared:nodes`, `:shared:search`, `:shared:transfers`. Lint via `:lint`; tests via `:core-test` and `:core-ui-test`.

Notable external libs: MEGA core-ui and analytics, Jetpack Compose (BOM + activity/viewmodel), `androidx.navigation3.runtime` (Navigation 3), `androidx.material3.adaptive.navigation.suite`, Hilt navigation, `kotlinx.serialization` (NavKey serialization), compose-state-events, Timber.

Convention plugins: `mega.android.library`, `mega.android.library.compose`, `mega.android.hilt`, plus Kotlin serialization.

## Testing
Unit tests use JUnit 5 + Mockito + Turbine + Truth (see root `.claude/CLAUDE.md` for conventions). Run:

```
./gradlew feature:cloudexplorer:testDebugUnitTest
```

## Notes & Gotchas
- Navigation uses Navigation 3 (`NavKey` + `EntryProviderScope.entry<...>`); NavKeys/result keys/result types are defined in `:navigation`, not here. Add new destinations by extending `CloudExplorerFeatureDestination.navigationGraph`.
- `NodesExplorerNavKey` is the shared explorer entry point: it branches behaviour on `startNavKey` (Copy/Move/Import/ImportAlbum/AddVideoToPlaylist/ShareTextToMega), so changes there affect every flow that reuses the explorer.
- Results are returned via `NavigationHandler.returnResult(<NavKey>.RESULT, value)` and observed by the caller through `monitorResult`/`clearResult`; uploads are triggered via `TransferHandler.setTransferEvent`.
- ViewModels with screen arguments use Hilt assisted injection (`@AssistedFactory` `Factory` + `Args`), e.g. `ShareFilesToMegaViewModel` and `UploadScannedDocumentsViewModel`.
- Lint disables `CoroutineCreationDuringComposition` and has `abortOnError = true`.
