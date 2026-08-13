package mega.privacy.android.feature.sync.ui.synclist

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.banner.InlineWarningBanner
import mega.android.core.ui.components.chip.MegaChip
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuAction
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.ui.SyncIssueNotificationViewModel
import mega.privacy.android.feature.sync.ui.model.StalledIssueUiItem
import mega.privacy.android.feature.sync.ui.synclist.SyncChip.SOLVED_ISSUES
import mega.privacy.android.feature.sync.ui.synclist.SyncChip.STALLED_ISSUES
import mega.privacy.android.feature.sync.ui.synclist.SyncChip.SYNC_FOLDERS
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersRoute
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersUiState
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.SyncStalledIssuesState
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersViewModel
import mega.privacy.android.feature.sync.ui.synclist.folders.TEST_TAG_SYNC_LIST_SCREEN_FAB
import mega.privacy.android.feature.sync.ui.synclist.solvedissues.SyncSolvedIssuesRoute
import mega.privacy.android.feature.sync.ui.synclist.solvedissues.SyncSolvedIssuesViewModel
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.SyncStalledIssuesRoute
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.SyncStalledIssuesViewModel
import mega.privacy.android.feature.sync.ui.views.SyncNotificationWarningBanner
import mega.privacy.android.feature.sync.ui.views.SyncPermissionWarningBanner
import mega.privacy.android.feature.sync.ui.views.SyncStorageQuotaExceedWarning
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.original.core.ui.controls.buttons.MegaMultiFloatingActionButton
import mega.privacy.android.shared.original.core.ui.controls.buttons.MultiFloatingActionButtonItem
import mega.privacy.android.shared.original.core.ui.controls.buttons.MultiFloatingActionButtonState
import mega.privacy.android.shared.original.core.ui.controls.buttons.rememberMultiFloatingActionButtonState
import mega.privacy.android.shared.original.core.ui.theme.extensions.conditional
import mega.privacy.android.shared.original.core.ui.utils.ComposableLifecycle
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.shared.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.mobile.analytics.event.AndroidBackupFABButtonPressedEvent
import mega.privacy.mobile.analytics.event.AndroidSyncFABButtonEvent
import mega.privacy.mobile.analytics.event.AndroidSyncMultiFABButtonPressedEvent
import mega.privacy.mobile.analytics.event.SyncListFoldersButtonPressedEvent
import mega.privacy.mobile.analytics.event.SyncListIssuesButtonPressedEvent
import mega.privacy.mobile.analytics.event.SyncListSolvedIssuesButtonPressedEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SyncListScreen(
    stalledIssuesCount: Int,
    onSyncFolderClicked: () -> Unit,
    onBackupFolderClicked: () -> Unit,
    onOpenMegaFolderClicked: (handle: Long) -> Unit,
    onCameraUploadsSettingsClicked: () -> Unit,
    syncPermissionsManager: SyncPermissionsManager,
    actions: List<MenuAction>,
    onActionPressed: (MenuAction) -> Unit,
    onSelectStopBackupDestinationClicked: (String?) -> Unit,
    onOpenUpgradeAccountClicked: () -> Unit,
    syncFoldersViewModel: SyncFoldersViewModel,
    syncStalledIssuesViewModel: SyncStalledIssuesViewModel,
    syncSolvedIssuesViewModel: SyncSolvedIssuesViewModel,
    syncIssueNotificationViewModel: SyncIssueNotificationViewModel,
    title: String,
    onStalledIssueMoreClicked: (issueId: String) -> Unit,
    isInCloudDrive: Boolean = false,
    selectedChip: SyncChip = SYNC_FOLDERS,
    onFabExpanded: (Boolean) -> Unit = {},
) {
    val onBackPressedDispatcher =
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val syncFoldersState by syncFoldersViewModel.uiState.collectAsStateWithLifecycle()
    val syncStalledIssueState by syncStalledIssuesViewModel.state.collectAsStateWithLifecycle()

    var isWarningBannerDisplayed by rememberSaveable { mutableStateOf(false) }
    ComposableLifecycle { event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            isWarningBannerDisplayed =
                syncPermissionsManager.isDisableBatteryOptimizationGranted().not()
        }
    }

    val multiFabState = rememberMultiFloatingActionButtonState()

    DisposableEffect(multiFabState.value) {
        onFabExpanded(multiFabState.value == MultiFloatingActionButtonState.EXPANDED)
        onDispose { }
    }

    MegaScaffold(
        contentWindowInsets = if (isInCloudDrive) WindowInsets(0.dp) else ScaffoldDefaults.contentWindowInsets,
        topBar = {
            if (!isInCloudDrive) {
                MegaTopAppBar(
                    title = title.ifEmpty { stringResource(R.string.sync_toolbar_title) },
                    navigationType = AppBarNavigationType.Back {
                        onBackPressedDispatcher?.onBackPressed()
                    },
                    actions = actions.map { action ->
                        MenuActionWithClick(action) { onActionPressed(action) }
                    },
                    // M3 draws the separator from scroll state rather than a fixed elevation.
                    drawBottomLineOnScrolledContent = isWarningBannerDisplayed ||
                            syncFoldersState.isWarningBannerDisplayed,
                )
            }
        },
        floatingActionButton = {
            if ((syncFoldersState.syncUiItems.isNotEmpty() || syncFoldersState.isLoading) && syncFoldersState.isStorageOverQuota.not()) {
                MegaMultiFloatingActionButton(
                    items = listOf(
                        MultiFloatingActionButtonItem(
                            icon = painterResource(id = iconPackR.drawable.ic_sync_01_medium_thin_outline),
                            label = stringResource(id = R.string.sync_toolbar_title),
                            onClicked = {
                                Analytics.tracker.trackEvent(AndroidSyncFABButtonEvent)
                                onSyncFolderClicked()
                                multiFabState.value = MultiFloatingActionButtonState.COLLAPSED
                            },
                        ),
                        MultiFloatingActionButtonItem(
                            icon = painterResource(id = iconPackR.drawable.ic_database_medium_thin_outline),
                            label = stringResource(id = sharedR.string.sync_add_new_backup_toolbar_title),
                            onClicked = {
                                Analytics.tracker.trackEvent(AndroidBackupFABButtonPressedEvent)
                                onBackupFolderClicked()
                                multiFabState.value = MultiFloatingActionButtonState.COLLAPSED
                            },
                        ),
                    ),
                    modifier = Modifier
                        .testTag(TEST_TAG_SYNC_LIST_SCREEN_FAB)
                        .navigationBarsPadding(),
                    multiFabState = multiFabState,
                    onStateChanged = { state ->
                        if (state == MultiFloatingActionButtonState.EXPANDED) {
                            Analytics.tracker.trackEvent(
                                AndroidSyncMultiFABButtonPressedEvent
                            )
                        }
                        onFabExpanded(state == MultiFloatingActionButtonState.EXPANDED)
                        multiFabState.value = state
                    },
                    isCircular = false,
                )
            }
        },
        content = { paddingValues ->
            SyncListScreenContent(
                syncFoldersUiState = syncFoldersState,
                syncStalledIssuesState = syncStalledIssueState,
                modifier = Modifier
                    .padding(paddingValues)
                    .conditional(multiFabState.value == MultiFloatingActionButtonState.EXPANDED) {
                        clickable(
                            interactionSource = null,
                            indication = null,
                        ) {
                            multiFabState.value = MultiFloatingActionButtonState.COLLAPSED
                        }
                    },
                stalledIssuesCount = stalledIssuesCount,
                moreClicked = { stalledIssueItem ->
                    onStalledIssueMoreClicked(stalledIssueItem.id)
                },
                onAddNewSyncClicked = onSyncFolderClicked,
                onAddNewBackupClicked = onBackupFolderClicked,
                onOpenMegaFolderClicked = onOpenMegaFolderClicked,
                syncPermissionsManager = syncPermissionsManager,
                onSelectStopBackupDestinationClicked = onSelectStopBackupDestinationClicked,
                onOpenUpgradeAccountClicked = onOpenUpgradeAccountClicked,
                onCameraUploadsSettingsClicked = onCameraUploadsSettingsClicked,
                syncFoldersViewModel = syncFoldersViewModel,
                syncStalledIssuesViewModel = syncStalledIssuesViewModel,
                syncSolvedIssuesViewModel = syncSolvedIssuesViewModel,
                syncIssueNotificationViewModel = syncIssueNotificationViewModel,
                deviceName = title,
                selectedChip = selectedChip,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
private fun SyncListScreenContent(
    modifier: Modifier,
    syncFoldersUiState: SyncFoldersUiState,
    syncStalledIssuesState: SyncStalledIssuesState,
    stalledIssuesCount: Int,
    moreClicked: (StalledIssueUiItem) -> Unit,
    onAddNewSyncClicked: () -> Unit,
    onAddNewBackupClicked: () -> Unit,
    onOpenMegaFolderClicked: (handle: Long) -> Unit,
    onCameraUploadsSettingsClicked: () -> Unit,
    syncPermissionsManager: SyncPermissionsManager,
    onSelectStopBackupDestinationClicked: (String?) -> Unit,
    onOpenUpgradeAccountClicked: () -> Unit,
    syncFoldersViewModel: SyncFoldersViewModel,
    syncStalledIssuesViewModel: SyncStalledIssuesViewModel,
    syncSolvedIssuesViewModel: SyncSolvedIssuesViewModel,
    syncIssueNotificationViewModel: SyncIssueNotificationViewModel,
    deviceName: String,
    selectedChip: SyncChip = SYNC_FOLDERS,
) {
    var checkedChip by rememberSaveable { mutableStateOf(selectedChip) }

    val syncSolvedIssuesState by syncSolvedIssuesViewModel.state.collectAsStateWithLifecycle()
    val issueNotificationState by syncIssueNotificationViewModel.state.collectAsStateWithLifecycle()

    val isSyncNotEmpty = syncFoldersUiState.syncUiItems.isNotEmpty()

    Column(modifier) {
        if (syncFoldersUiState.isStorageOverQuota) {
            SyncStorageQuotaExceedWarning(onUpgradeClick = onOpenUpgradeAccountClicked)
        } else {
            SyncPermissionWarningBanner(
                syncPermissionsManager = syncPermissionsManager,
                isDisableBatteryOptimizationEnabled = syncFoldersUiState.isDisableBatteryOptimizationEnabled
            )
            SyncNotificationWarningBanner(
                issueNotificationState,
                onDismissNotification = syncIssueNotificationViewModel::dismissNotification,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (syncFoldersUiState.syncUiItems.isNotEmpty() && syncFoldersUiState.isLowBatteryLevel) {
                InlineWarningBanner(
                    body = stringResource(id = sharedR.string.general_message_sync_paused_low_battery_level),
                    showCancelButton = false,
                )
            }
        }
        if (checkedChip != SYNC_FOLDERS || syncStalledIssuesState.stalledIssues.isNotEmpty() || syncSolvedIssuesState.solvedIssues.isNotEmpty()) {
            if (syncFoldersUiState.syncUiItems.isNotEmpty()) {
                // deferred loading of HeaderChips to avoid showing chips first before sync items are loaded
                HeaderChips(
                    selectedChip = checkedChip,
                    stalledIssuesCount = stalledIssuesCount,
                    onChipSelected = { checkedChip = it })
            }
        }

        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            isRefreshing = isSyncNotEmpty && syncFoldersUiState.isRefreshing,
            onRefresh = {
                if (isSyncNotEmpty) syncFoldersViewModel.onSyncRefresh()
            },
        ) {
            SelectedChipScreen(
                onAddNewSyncClicked = onAddNewSyncClicked,
                onAddNewBackupClicked = onAddNewBackupClicked,
                onSelectStopBackupDestinationClicked = onSelectStopBackupDestinationClicked,
                onOpenMegaFolderClicked = onOpenMegaFolderClicked,
                onCameraUploadsSettingsClicked = onCameraUploadsSettingsClicked,
                moreClicked = moreClicked,
                issuesInfoClicked = {
                    checkedChip = STALLED_ISSUES
                },
                checkedChip = checkedChip,
                syncStalledIssuesViewModel = syncStalledIssuesViewModel,
                syncFoldersViewModel = syncFoldersViewModel,
                syncSolvedIssuesViewModel = syncSolvedIssuesViewModel,
                syncFoldersUiState = syncFoldersUiState,
                deviceName = deviceName,
            )
        }
    }
}

@Composable
private fun HeaderChips(
    selectedChip: SyncChip,
    stalledIssuesCount: Int,
    onChipSelected: (SyncChip) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MegaChip(
            selected = selectedChip == SYNC_FOLDERS,
            text = stringResource(id = R.string.sync_folders),
            modifier = modifier.testTag(SYNC_FOLDERS_CHIP_TEST_TAG),
            onClick = {
                Analytics.tracker.trackEvent(SyncListFoldersButtonPressedEvent)
                onChipSelected(SYNC_FOLDERS)
            },
        )
        MegaChip(
            selected = selectedChip == STALLED_ISSUES,
            text = if (stalledIssuesCount > 0) {
                stringResource(R.string.sync_stalled_issues, stalledIssuesCount)
            } else {
                stringResource(id = R.string.sync_stalled_issue_zero)
            },
            modifier = modifier.testTag(STALLED_ISSUES_CHIP_TEST_TAG),
            onClick = {
                Analytics.tracker.trackEvent(SyncListIssuesButtonPressedEvent)
                onChipSelected(STALLED_ISSUES)
            },
        )
        MegaChip(
            selected = selectedChip == SOLVED_ISSUES,
            text = stringResource(id = sharedR.string.device_center_sync_solved_issues_chip_text),
            modifier = modifier.testTag(SOLVED_ISSUES_CHIP_TEST_TAG),
            onClick = {
                Analytics.tracker.trackEvent(SyncListSolvedIssuesButtonPressedEvent)
                onChipSelected(SOLVED_ISSUES)
            },
        )
    }
}

@Composable
private fun SelectedChipScreen(
    onAddNewSyncClicked: () -> Unit,
    onAddNewBackupClicked: () -> Unit,
    onSelectStopBackupDestinationClicked: (String?) -> Unit,
    onOpenMegaFolderClicked: (handle: Long) -> Unit,
    onCameraUploadsSettingsClicked: () -> Unit,
    moreClicked: (StalledIssueUiItem) -> Unit,
    issuesInfoClicked: () -> Unit,
    checkedChip: SyncChip,
    syncFoldersViewModel: SyncFoldersViewModel,
    syncStalledIssuesViewModel: SyncStalledIssuesViewModel,
    syncSolvedIssuesViewModel: SyncSolvedIssuesViewModel,
    syncFoldersUiState: SyncFoldersUiState,
    deviceName: String,
) {
    when (checkedChip) {
        SYNC_FOLDERS -> {
            SyncFoldersRoute(
                onAddNewSyncClicked = onAddNewSyncClicked,
                onAddNewBackupClicked = onAddNewBackupClicked,
                onSelectStopBackupDestinationClicked = onSelectStopBackupDestinationClicked,
                issuesInfoClicked = issuesInfoClicked,
                viewModel = syncFoldersViewModel,
                uiState = syncFoldersUiState,
                deviceName = deviceName,
                onOpenMegaFolderClicked = onOpenMegaFolderClicked,
                onCameraUploadsSettingsClicked = onCameraUploadsSettingsClicked,
            )
        }

        STALLED_ISSUES -> {
            SyncStalledIssuesRoute(
                moreClicked = moreClicked,
                viewModel = syncStalledIssuesViewModel
            )
        }

        SOLVED_ISSUES -> {
            SyncSolvedIssuesRoute(viewModel = syncSolvedIssuesViewModel)
        }
    }
}

/**
 * Sync Folders Chip test tag
 */
internal const val SYNC_FOLDERS_CHIP_TEST_TAG = "sync_list:folders_chip"

/**
 * Stalled Issues Chip test tag
 */
internal const val STALLED_ISSUES_CHIP_TEST_TAG = "sync_list:stalled_issues_chip"

/**
 * Solved Issues Chip test tag
 */
internal const val SOLVED_ISSUES_CHIP_TEST_TAG = "sync_list:solved_issues_chip"

internal const val BOTTOM_PADDING = 72
