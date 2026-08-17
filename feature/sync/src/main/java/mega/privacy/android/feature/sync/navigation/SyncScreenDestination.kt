package mega.privacy.android.feature.sync.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import mega.android.core.ui.theme.thememode.LocalIsDark
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.feature.sync.ui.SyncEmptyScreen
import mega.privacy.android.feature.sync.ui.megapicker.MegaPickerRoute
import mega.privacy.android.feature.sync.ui.megapicker.MegaPickerViewModel
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderAction
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderScreenRoute
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderViewModel
import mega.privacy.android.feature.sync.ui.settings.SettingsSyncRoute
import mega.privacy.android.feature.sync.ui.synclist.SyncChip
import mega.privacy.android.feature.sync.ui.synclist.SyncListAction
import mega.privacy.android.feature.sync.ui.synclist.SyncListRoute
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.SyncStalledIssuesViewModel
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.trackResolutionConfirmed
import mega.privacy.android.feature.sync.ui.views.ApplyToAllDialog
import mega.privacy.android.feature.sync.ui.views.IssuesResolutionDialog
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.bottomsheet.bottomSheetMetadata
import mega.privacy.android.navigation.contract.navOptions
import mega.privacy.android.navigation.destination.CloudDriveNavKey
import mega.privacy.android.navigation.destination.SelectStopBackupDestinationNavKey
import mega.privacy.android.navigation.destination.SelectSyncFolderNavKey
import mega.privacy.android.navigation.destination.SettingsCameraUploadsNavKey
import mega.privacy.android.navigation.destination.SyncApplyToAllNavKey
import mega.privacy.android.navigation.destination.SyncEmptyRouteNavKey
import mega.privacy.android.navigation.destination.SyncListNavKey
import mega.privacy.android.navigation.destination.SyncMegaPickerNavKey
import mega.privacy.android.navigation.destination.SyncNewFolderNavKey
import mega.privacy.android.navigation.destination.SyncSelectStopBackupDestinationNavKey
import mega.privacy.android.navigation.destination.SyncSettingsNavKey
import mega.privacy.android.navigation.destination.SyncStalledIssueResolutionNavKey
import mega.privacy.android.navigation.destination.SyncTab
import mega.privacy.android.shared.nodes.mapper.FileTypeIconMapper
import mega.privacy.android.shared.original.core.ui.navigation.launchFolderPicker
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.utils.findFragmentActivity
import mega.privacy.android.shared.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.mobile.analytics.event.AddSyncScreenEvent
import mega.privacy.mobile.analytics.event.AndroidSyncGetStartedButtonEvent
import timber.log.Timber

fun EntryProviderScope<NavKey>.syncScreens(
    navigationHandler: NavigationHandler,
    fileTypeIconMapper: FileTypeIconMapper,
    syncPermissionsManager: SyncPermissionsManager,
    getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    openUpgradeAccountPage: () -> Unit,
) {
    entry<SyncListNavKey> { navKey ->
        val useCloudExplorerPicker by produceState(initialValue = false) {
            value = runCatching {
                getFeatureFlagValueUseCase(AppFeatures.CloudExplorer)
            }.getOrDefault(false)
        }
        SyncLegacyTheme {
            SyncListRoute(
                onStalledIssueMoreClicked = { issueId ->
                    navigationHandler.navigate(
                        SyncStalledIssueResolutionNavKey(issueId = issueId)
                    )
                },
                syncPermissionsManager = syncPermissionsManager,
                onSyncFolderClicked = {
                    navigationHandler.navigate(SyncNewFolderNavKey(syncType = SyncType.TYPE_TWOWAY))
                },
                onBackupFolderClicked = {
                    navigationHandler.navigate(SyncNewFolderNavKey(syncType = SyncType.TYPE_BACKUP))
                },
                onSelectStopBackupDestinationClicked = { folderName ->
                    navigationHandler.navigate(
                        if (useCloudExplorerPicker) {
                            SelectStopBackupDestinationNavKey(folderName = folderName)
                        } else {
                            SyncSelectStopBackupDestinationNavKey(folderName = folderName)
                        }
                    )
                },
                onOpenUpgradeAccountClicked = openUpgradeAccountPage,
                selectedChip = when (navKey.initialTab) {
                    SyncTab.FOLDERS -> SyncChip.SYNC_FOLDERS
                    SyncTab.STALLED_ISSUES -> SyncChip.STALLED_ISSUES
                    SyncTab.SOLVED_ISSUES -> SyncChip.SOLVED_ISSUES
                },
                onOpenMegaFolderClicked = { handle ->
                    navigationHandler.navigate(CloudDriveNavKey(nodeHandle = handle))
                },
                onCameraUploadsSettingsClicked = {
                    navigationHandler.navigate(SettingsCameraUploadsNavKey)
                },
                onSyncSettingsClicked = {
                    navigationHandler.navigate(SyncSettingsNavKey)
                },
            )
        }
    }

    entry<SyncNewFolderNavKey> { navKey ->
        val useCloudExplorerPicker by produceState(initialValue = false) {
            value = runCatching {
                getFeatureFlagValueUseCase(AppFeatures.CloudExplorer)
            }.getOrDefault(false)
        }
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

        SyncLegacyTheme {
            SyncNewFolderScreenRoute(
                viewModel = viewModel,
                syncPermissionsManager = syncPermissionsManager,
                openSelectMegaFolderScreen = {
                    navigationHandler.navigate(
                        if (useCloudExplorerPicker) {
                            SelectSyncFolderNavKey
                        } else {
                            SyncMegaPickerNavKey
                        }
                    )
                },
                openNextScreen = { _ ->
                    if (navKey.isFromDeviceCenter) {
                        navigationHandler.navigate(SyncListNavKey())
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
            )
        }
    }

    entry<SyncMegaPickerNavKey> {
        val viewModel =
            hiltViewModel<MegaPickerViewModel, MegaPickerViewModel.MegaPickerViewModelFactory> { factory ->
                factory.create(isStopBackup = false, folderName = "")
            }
        SyncLegacyTheme {
            MegaPickerRoute(
                viewModel = viewModel,
                syncPermissionsManager = syncPermissionsManager,
                folderSelected = { navigationHandler.back() },
                backClicked = { navigationHandler.back() },
                fileTypeIconMapper = fileTypeIconMapper,
            )
        }
    }

    entry<SyncSelectStopBackupDestinationNavKey> { navKey ->
        val viewModel =
            hiltViewModel<MegaPickerViewModel, MegaPickerViewModel.MegaPickerViewModelFactory> { factory ->
                factory.create(isStopBackup = true, folderName = navKey.folderName)
            }
        SyncLegacyTheme {
            MegaPickerRoute(
                viewModel = viewModel,
                syncPermissionsManager = syncPermissionsManager,
                folderSelected = { navigationHandler.back() },
                backClicked = { navigationHandler.back() },
                fileTypeIconMapper = fileTypeIconMapper,
                isStopBackupMegaPicker = true,
            )
        }
    }

    entry<SyncEmptyRouteNavKey> {
        LaunchedEffect(Unit) {
            Analytics.tracker.trackEvent(AddSyncScreenEvent)
        }
        SyncLegacyTheme {
            SyncEmptyScreen {
                Analytics.tracker.trackEvent(AndroidSyncGetStartedButtonEvent)
                navigationHandler.navigate(SyncNewFolderNavKey())
            }
        }
    }

    entry<SyncSettingsNavKey> {
        SyncLegacyTheme {
            SettingsSyncRoute()
        }
    }

    entry<SyncStalledIssueResolutionNavKey>(metadata = bottomSheetMetadata()) { key ->
        val viewModel: SyncStalledIssuesViewModel =
            hiltViewModel(viewModelStoreOwner = sharedViewModelStoreOwner())
        val state by viewModel.state.collectAsStateWithLifecycle()

        SyncLegacyTheme {
            state.stalledIssues.firstOrNull { it.id == key.issueId }?.let { issue ->
                IssuesResolutionDialog(
                    icon = issue.icon,
                    conflictName = issue.conflictName,
                    nodeName = issue.displayedName,
                    actions = issue.actions,
                    actionSelected = { action ->
                        navigationHandler.navigate(
                            SyncApplyToAllNavKey(
                                issueId = issue.id,
                                actionType = action.resolutionActionType.name,
                            ),
                            navOptions {
                                popUpTo<SyncStalledIssueResolutionNavKey> { inclusive = true }
                            },
                        )
                    },
                )
            }
        }
    }

    entry<SyncApplyToAllNavKey>(metadata = DialogSceneStrategy.dialog()) { key ->
        val viewModel: SyncStalledIssuesViewModel =
            hiltViewModel(viewModelStoreOwner = sharedViewModelStoreOwner())
        val state by viewModel.state.collectAsStateWithLifecycle()
        val issue = state.stalledIssues.firstOrNull { it.id == key.issueId }
        val action = issue?.actions?.firstOrNull { it.resolutionActionType.name == key.actionType }

        SyncLegacyTheme {
            if (issue != null && action != null) {
                ApplyToAllDialog(
                    fileName = issue.displayedName,
                    selectedAction = action,
                    onApplyToCurrent = {
                        action.resolutionActionType.trackResolutionConfirmed()
                        viewModel.handleAction(
                            SyncListAction.ResolveStalledIssue(issue, action, isApplyToAll = false)
                        )
                        navigationHandler.remove(key)
                    },
                    onApplyToAll = {
                        action.resolutionActionType.trackResolutionConfirmed()
                        viewModel.handleAction(
                            SyncListAction.ResolveStalledIssue(issue, action, isApplyToAll = true)
                        )
                        navigationHandler.remove(key)
                    },
                    onCancel = { navigationHandler.remove(key) },
                    shouldShowApplyToAllOption = state.stalledIssues.size > 1,
                )
            }
        }
    }
}

/**
 * The stalled issues view model is shared with the sync list, so both must resolve it against the
 * same store owner. Mirrors what [mega.privacy.android.feature.sync.ui.synclist.SyncListRoute] does.
 */
@Composable
private fun sharedViewModelStoreOwner(): ViewModelStoreOwner =
    LocalContext.current.findFragmentActivity() ?: checkNotNull(LocalViewModelStoreOwner.current)

/**
 * Supplies the Material 2 theme that the not-yet-migrated original-core-ui composables still read.
 *
 * The root [mega.android.core.ui.theme.AndroidTheme] in MegaActivity already themes the whole nav
 * display and publishes the resolved mode as [LocalIsDark], so entries reuse that instead of each
 * subscribing to the theme preference again. Delete the wrapper from an entry once its content is
 * fully core-ui.
 */
@Composable
private fun SyncLegacyTheme(content: @Composable () -> Unit) {
    OriginalTheme(isDark = LocalIsDark.current, content = content)
}
