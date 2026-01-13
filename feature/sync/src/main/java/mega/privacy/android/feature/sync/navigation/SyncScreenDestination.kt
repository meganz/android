package mega.privacy.android.feature.sync.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.feature.sync.ui.SyncEmptyScreen
import mega.privacy.android.feature.sync.ui.isDarkMode
import mega.privacy.android.feature.sync.ui.megapicker.MegaPickerRoute
import mega.privacy.android.feature.sync.ui.megapicker.MegaPickerViewModel
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderAction
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderScreenRoute
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderViewModel
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.android.feature.sync.ui.synclist.SyncChip
import mega.privacy.android.feature.sync.ui.synclist.SyncListRoute
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.CloudDriveNavKey
import mega.privacy.android.navigation.destination.SettingsCameraUploadsNavKey
import mega.privacy.android.navigation.destination.SyncEmptyRouteNavKey
import mega.privacy.android.navigation.destination.SyncListNavKey
import mega.privacy.android.navigation.destination.SyncMegaPickerNavKey
import mega.privacy.android.navigation.destination.SyncNewFolderNavKey
import mega.privacy.android.navigation.destination.SyncSelectStopBackupDestinationNavKey
import mega.privacy.android.shared.original.core.ui.navigation.launchFolderPicker
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.mobile.analytics.event.AddSyncScreenEvent
import mega.privacy.mobile.analytics.event.AndroidSyncGetStartedButtonEvent
import timber.log.Timber

fun EntryProviderScope<NavKey>.syncScreens(
    navigationHandler: NavigationHandler,
    fileTypeIconMapper: FileTypeIconMapper,
    syncPermissionsManager: SyncPermissionsManager,
    monitorThemeModeUseCase: MonitorThemeModeUseCase,
    openUpgradeAccountPage: () -> Unit,
) {
    entry<SyncListNavKey> {
        val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
        OriginalTheme(isDark = themeMode.isDarkMode()) {
            SyncListRoute(
                syncPermissionsManager = syncPermissionsManager,
                onSyncFolderClicked = {
                    navigationHandler.navigate(SyncNewFolderNavKey(syncType = SyncType.TYPE_TWOWAY))
                },
                onBackupFolderClicked = {
                    navigationHandler.navigate(SyncNewFolderNavKey(syncType = SyncType.TYPE_BACKUP))
                },
                onSelectStopBackupDestinationClicked = { folderName ->
                    navigationHandler.navigate(SyncSelectStopBackupDestinationNavKey(folderName = folderName))
                },
                onOpenUpgradeAccountClicked = openUpgradeAccountPage,
                selectedChip = SyncChip.SYNC_FOLDERS,
                onOpenMegaFolderClicked = { handle ->
                    navigationHandler.navigate(CloudDriveNavKey(nodeHandle = handle))
                },
                onCameraUploadsSettingsClicked = {
                    navigationHandler.navigate(SettingsCameraUploadsNavKey)
                },
                isSingleActivity = true,
            )
        }
    }

    entry<SyncNewFolderNavKey> { navKey ->
        val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
        val context = LocalContext.current
        val viewModel =
            hiltViewModel<SyncNewFolderViewModel, SyncNewFolderViewModel.SyncNewFolderViewModelFactory> { factory ->
                factory.create(
                    syncType = navKey.syncType,
                    remoteFolderHandle = navKey.remoteFolderHandle,
                    remoteFolderName = navKey.remoteFolderName
                )
            }

        val launcher = launchFolderPicker(
            onFolderSelected = { uri ->
                runCatching {
                    val documentFile = DocumentFile.fromTreeUri(context, uri)
                    documentFile?.let {
                        viewModel.handleAction(
                            SyncNewFolderAction.LocalFolderSelected(
                                documentFile
                            )
                        )
                    }
                }.onFailure {
                    Timber.e(it)
                }
            },
        )

        OriginalTheme(isDark = themeMode.isDarkMode()) {
            SyncNewFolderScreenRoute(
                viewModel = viewModel,
                syncPermissionsManager = syncPermissionsManager,
                openSelectMegaFolderScreen = {
                    navigationHandler.navigate(SyncMegaPickerNavKey)
                },
                openNextScreen = { _ ->
                    if (navKey.isFromDeviceCenter) {
                        navigationHandler.navigate(SyncListNavKey)
                    }
                    navigationHandler.remove(navKey)
                },
                openUpgradeAccount = openUpgradeAccountPage,
                onBackClicked = {
                    navigationHandler.back()
                },
                onSelectFolder = {
                    launcher.launch(null)
                },
                isSingleActivity = true,
            )
        }
    }

    entry<SyncMegaPickerNavKey> {
        val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
        val viewModel =
            hiltViewModel<MegaPickerViewModel, MegaPickerViewModel.MegaPickerViewModelFactory> { factory ->
                factory.create(isStopBackup = false, folderName = "")
            }
        OriginalTheme(isDark = themeMode.isDarkMode()) {
            MegaPickerRoute(
                viewModel = viewModel,
                syncPermissionsManager = syncPermissionsManager,
                folderSelected = { navigationHandler.back() },
                backClicked = { navigationHandler.back() },
                fileTypeIconMapper = fileTypeIconMapper,
                isSingleActivity = true,
            )
        }
    }

    entry<SyncSelectStopBackupDestinationNavKey> { navKey ->
        val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
        val viewModel =
            hiltViewModel<MegaPickerViewModel, MegaPickerViewModel.MegaPickerViewModelFactory> { factory ->
                factory.create(isStopBackup = true, folderName = navKey.folderName)
            }
        OriginalTheme(isDark = themeMode.isDarkMode()) {
            MegaPickerRoute(
                viewModel = viewModel,
                syncPermissionsManager = syncPermissionsManager,
                folderSelected = { navigationHandler.back() },
                backClicked = { navigationHandler.back() },
                fileTypeIconMapper = fileTypeIconMapper,
                isStopBackupMegaPicker = true,
                isSingleActivity = true,
            )
        }
    }

    entry<SyncEmptyRouteNavKey> {
        val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
        LaunchedEffect(Unit) {
            Analytics.tracker.trackEvent(AddSyncScreenEvent)
        }
        OriginalTheme(isDark = themeMode.isDarkMode()) {
            SyncEmptyScreen {
                Analytics.tracker.trackEvent(AndroidSyncGetStartedButtonEvent)
                navigationHandler.navigate(SyncNewFolderNavKey())
            }
        }
    }
}
