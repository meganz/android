# CLAUDE.md

This file provides guidance to Claude Code when working in the `:feature:sync` module.

> Module path: `:feature:sync` · Build file: `feature/sync/sync.gradle.kts` · Namespace: `mega.privacy.android.feature.sync`

## Overview
`:feature:sync` implements the two-way folder Sync and Backup feature: pairing a local device folder with a MEGA cloud folder, monitoring the sync engine, surfacing stalled/solved issues, and handling background scheduling. It covers the full user journey — picking a MEGA folder, creating a new folder pair, configuring sync settings, viewing folder/issue lists, and renaming/stopping backups — plus the notification and WorkManager plumbing that keeps syncs running.

It is a self-contained feature module spanning all three Clean Architecture layers, consumed by the app module via the Navigation3 `FeatureDestination` contract.

## Architecture & Layout
- `data/` — repository implementations (`*RepositoryImpl`), SDK gateways (`*Gateway`/`*GatewayImpl`), `model/`, and `mapper/` (incl. `notification/`, `solvedissue/`, `stalledissue/`).
- `domain/` — `entity/` (incl. `megapicker/`), `repository/` interfaces, `exception/`, `mapper/`, and `usecase/` (~54 use cases grouped under `backup/`, `logout/`, `megapicker/`, `notifcation/`, `solvedissue/`, `stalledIssue/`, `sync/`).
- `ui/` — Compose screens, routes, and UI models per sub-flow: `megapicker/`, `newfolderpair/`, `settings/`, `synclist/` (with `folders/`, `solvedissues/`, `stalledissues/`), `createnewfolder/`, `renamebackup/`, `stopbackup/`, `notification/`, `permissions/`, plus `mapper/`, `formatter/`, `extension/`, `views/`.
- `di/` — Hilt modules: `SyncDataModule`, `SyncDomainModule`, `NotificationModule`, `SyncNavigationModule`.
- `navigation/` — `FeatureDestination` entry point and per-screen destinations / NavKeys + deeplink processing.

## Key Components
- **ViewModels**: `SyncViewModel`, `SyncListViewModel`, `SyncFoldersViewModel`, `SyncStalledIssuesViewModel`, `SyncSolvedIssuesViewModel`, `MegaPickerViewModel`, `SyncNewFolderViewModel`, `CreateNewFolderViewModel`, `RenameAndCreateBackupViewModel`, `StopBackupViewModel`, `SettingsSyncViewModel`, `SyncMonitorViewModel`, `SyncIssueNotificationViewModel`, `SyncPromotionViewModel`.
- **Use Cases**: e.g. `MonitorSyncsUseCase`, `MonitorSyncStalledIssuesUseCase`, `MonitorSyncSolvedIssuesUseCase`, `GetFolderPairsUseCase`, `MonitorShouldSyncUseCase`, `IsOnboardingRequiredUseCase`, `StartSyncWorkerUseCase`/`StopSyncWorkerUseCase`, `ChangeSyncLocalRootUseCase`, `GetSyncDebrisSizeInBytesUseCase`, plus `notifcation/`, `megapicker/`, and `backup/` groups.
- **Repositories / Gateways / Data sources**: `SyncRepositoryImpl`, `SyncSolvedIssuesRepositoryImpl`, `SyncNotificationRepositoryImpl`, `SyncPreferencesRepositoryImpl`, `SyncPromotionPreferencesRepositoryImpl`, `SyncNewFolderParamsRepositoryImpl`, `SyncDebrisRepositoryImpl`. Gateways: `SyncGateway`, `SyncWorkManagerGateway`, `SyncNotificationGateway`, `SyncSolvedIssuesGateway`, `SyncStatsCacheGateway`, `UserPausedSyncGateway`, `SyncDebrisGateway`. Background work via `SyncWorker`.
- **Navigation**: `SyncFeatureDestination` (implements `FeatureDestination`, wires the Navigation3 `EntryProviderScope`); destinations `SyncListDestination`, `SyncScreenDestination`, `SyncNewFolderDestination`, `SyncMegaPickerDestination`, `StopBackupMegaPickerDestination`, `SyncEmptyDestination`; `SyncNavGraph` and `SyncDeeplinkProcessor`.
- **UI**: `SyncHomeScreen`, `SyncListScreen`, `SyncFoldersScreen`, `StalledIssuesScreen`, `SyncSolvedIssuesScreen`, `SyncNewFolderScreen`, `MegaPickerScreen`, `SyncEmptyScreen` (with matching `*Route` composables).

## Module Dependencies
- Internal: `:domain`, `:data`, `:navigation`, `:core:navigation-contract`, `:core:coroutine`, `:core:feature-flags`, `:core:formatter`, `:core:ui-components:node-components`, `:core:analytics:analytics-tracker`, `:shared:sync`, `:shared:nodes`, `:shared:original-core-ui`, `:legacy-core-ui`, `:resources:string-resources`, `:resources:icon-pack`; plus pre-built MEGA SDK.
- External: Compose (BOM + Material3 + activity), Navigation3 runtime, Hilt + `hilt-navigation` + `hilt-work`, WorkManager, DataStore preferences, Kotlinx serialization, Gson, Guava, Accompanist permissions, AndroidX lifecycle (incl. `lifecycle-service`), documentfile, Timber, MEGA analytics.

## Testing
Per project convention: JUnit5 + Mockito + Turbine + Truth (instrumentation via `HiltTestRunner`). Run: `./gradlew feature:sync:testDebugUnitTest`.

## Notes & Gotchas
- Build plugins: `mega.android.library`, `mega.android.library.compose`, `mega.android.room`, `mega.android.hilt`, kotlin serialization, and `kotlin-parcelize`. `lint { abortOnError = true }`.
- Uses Navigation3 (`androidx.navigation3.runtime`) with the `FeatureDestination` contract — new screens are registered through `SyncFeatureDestination`/destination classes, not a classic NavHost.
- The `notifcation` domain package is intentionally spelled that way (existing typo) — match it when adding files there.
- Background syncing runs through `SyncWorker` + `SyncWorkManagerGateway`; sync state is driven by the MEGA SDK via gateways, so prefer monitoring use cases (`Monitor*UseCase`) for reactive state.
- See root `.claude/CLAUDE.md` for global Clean Architecture, naming, and testing conventions.
