package mega.privacy.android.feature.sync.ui.synclist

import mega.privacy.android.shared.resources.R as sharedR
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionAction
import mega.privacy.android.feature.sync.ui.model.StalledIssueUiItem
import mega.privacy.android.feature.sync.ui.model.SyncModalSheetContent
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.android.feature.sync.ui.synclist.SyncChip.SOLVED_ISSUES
import mega.privacy.android.feature.sync.ui.synclist.SyncChip.STALLED_ISSUES
import mega.privacy.android.feature.sync.ui.synclist.SyncChip.SYNC_FOLDERS
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersRoute
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersState
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersViewModel
import mega.privacy.android.feature.sync.ui.synclist.solvedissues.SyncSolvedIssuesRoute
import mega.privacy.android.feature.sync.ui.synclist.solvedissues.SyncSolvedIssuesViewModel
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.SyncStalledIssuesRoute
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.SyncStalledIssuesViewModel
import mega.privacy.android.feature.sync.ui.views.ConflictDetailsDialog
import mega.privacy.android.feature.sync.ui.views.IssuesResolutionDialog
import mega.privacy.android.feature.sync.ui.views.SyncPermissionWarningBanner
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.banners.ActionBanner
import mega.privacy.android.shared.original.core.ui.controls.banners.WarningBanner
import mega.privacy.android.shared.original.core.ui.controls.chip.ChipBar
import mega.privacy.android.shared.original.core.ui.controls.chip.MegaChip
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.shared.original.core.ui.controls.snackbars.MegaSnackbar
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import mega.privacy.mobile.analytics.event.SyncListBannerUpgradeButtonPressedEvent

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun SyncListScreen(
    stalledIssuesCount: Int,
    addFolderClicked: () -> Unit,
    actionSelected: (item: StalledIssueUiItem, selectedAction: StalledIssueResolutionAction) -> Unit,
    snackBarHostState: SnackbarHostState,
    syncPermissionsManager: SyncPermissionsManager,
    actions: List<MenuAction>,
    onActionPressed: (MenuAction) -> Unit,
    onOpenUpgradeAccountClicked: () -> Unit,
    syncFoldersViewModel: SyncFoldersViewModel,
    syncStalledIssuesViewModel: SyncStalledIssuesViewModel,
    syncSolvedIssuesViewModel: SyncSolvedIssuesViewModel,
    title: String? = null,
    selectedChip: SyncChip = SYNC_FOLDERS,
) {
    val onBackPressedDispatcher =
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    var sheetContent by remember { mutableStateOf<SyncModalSheetContent?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val modalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmValueChange = { _ -> true }
    )

    BottomSheet(
        modalSheetState = modalSheetState,
        sheetBody = {
            when (val content = sheetContent) {
                is SyncModalSheetContent.DetailedInfo -> {
                    ConflictDetailsDialog(
                        content.stalledIssueUiItem.detailedInfo.title,
                        content.stalledIssueUiItem.detailedInfo.explanation,
                    )
                }

                is SyncModalSheetContent.IssueResolutions -> {
                    IssuesResolutionDialog(
                        icon = content.stalledIssueUiItem.icon,
                        conflictName = content.stalledIssueUiItem.conflictName,
                        nodeName = content.stalledIssueUiItem.nodeNames.firstOrNull()
                            ?: content.stalledIssueUiItem.localPaths.first(),
                        actions = content.stalledIssueUiItem.actions,
                        actionSelected = { action ->
                            actionSelected(content.stalledIssueUiItem, action)
                            coroutineScope.launch {
                                modalSheetState.hide()
                            }
                        }
                    )
                }

                else -> {

                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                MegaAppBar(
                    title = title ?: stringResource(R.string.sync_toolbar_title),
                    appBarType = AppBarType.BACK_NAVIGATION,
                    elevation = 0.dp,
                    onNavigationPressed = {
                        onBackPressedDispatcher?.onBackPressed()
                    },
                    actions = actions,
                    onActionPressed = onActionPressed
                )
            }, content = { paddingValues ->
                SyncListScreenContent(
                    modifier = Modifier
                        .padding(paddingValues),
                    stalledIssuesCount = stalledIssuesCount,
                    stalledIssuesDetailsClicked = { stalledIssueItem ->
                        sheetContent = SyncModalSheetContent.DetailedInfo(stalledIssueItem)
                        coroutineScope.launch {
                            modalSheetState.show()
                        }
                    },
                    moreClicked = { stalledIssueItem ->
                        sheetContent = SyncModalSheetContent.IssueResolutions(stalledIssueItem)
                        coroutineScope.launch {
                            modalSheetState.show()
                        }
                    },
                    addFolderClicked = addFolderClicked,
                    syncPermissionsManager = syncPermissionsManager,
                    onOpenUpgradeAccountClicked = onOpenUpgradeAccountClicked,
                    syncFoldersViewModel = syncFoldersViewModel,
                    syncStalledIssuesViewModel = syncStalledIssuesViewModel,
                    syncSolvedIssuesViewModel = syncSolvedIssuesViewModel,
                    selectedChip = selectedChip,
                )
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = snackBarHostState,
                    snackbar = { data ->
                        MegaSnackbar(snackbarData = data)
                    }
                )
            }
        )
    }
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SyncListScreenContent(
    modifier: Modifier,
    stalledIssuesCount: Int,
    stalledIssuesDetailsClicked: (StalledIssueUiItem) -> Unit,
    moreClicked: (StalledIssueUiItem) -> Unit,
    addFolderClicked: () -> Unit,
    syncPermissionsManager: SyncPermissionsManager,
    onOpenUpgradeAccountClicked: () -> Unit,
    syncFoldersViewModel: SyncFoldersViewModel,
    syncStalledIssuesViewModel: SyncStalledIssuesViewModel,
    syncSolvedIssuesViewModel: SyncSolvedIssuesViewModel,
    selectedChip: SyncChip = SYNC_FOLDERS,
) {
    var checkedChip by rememberSaveable { mutableStateOf(selectedChip) }

    val syncFoldersState by syncFoldersViewModel.uiState.collectAsStateWithLifecycle()
    val syncStalledIssuesState by syncStalledIssuesViewModel.state.collectAsStateWithLifecycle()
    val syncSolvedIssuesState by syncSolvedIssuesViewModel.state.collectAsStateWithLifecycle()

    val pullToRefreshState = rememberPullRefreshState(
        refreshing = syncFoldersState.isRefreshing,
        onRefresh = {
            syncFoldersViewModel.onSyncRefresh()
        })

    Column(modifier) {
        SyncPermissionWarningBanner(
            syncPermissionsManager = syncPermissionsManager
        )
        if (syncFoldersState.isFreeAccount && syncFoldersState.syncUiItems.isNotEmpty()) {
            ActionBanner(
                mainText = stringResource(id = sharedR.string.sync_error_banner_free_user),
                leftActionText = stringResource(sharedR.string.sync_error_storage_over_quota_banner_action),
                leftActionClicked = {
                    Analytics.tracker.trackEvent(SyncListBannerUpgradeButtonPressedEvent)
                    onOpenUpgradeAccountClicked()
                },
                modifier = Modifier.padding(top = 20.dp)
            )
            MegaDivider(
                dividerType = DividerType.FullSize,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        } else if (syncFoldersState.isStorageOverQuota) {
            ActionBanner(
                mainText = stringResource(sharedR.string.sync_error_storage_over_quota_banner_title),
                leftActionText = stringResource(sharedR.string.sync_error_storage_over_quota_banner_action),
                leftActionClicked = onOpenUpgradeAccountClicked,
                modifier = Modifier.padding(top = 20.dp)
            )
            MegaDivider(
                dividerType = DividerType.FullSize,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        } else if (syncFoldersState.syncUiItems.isNotEmpty() && syncFoldersState.isLowBatteryLevel) {
            WarningBanner(
                textString = stringResource(id = mega.privacy.android.shared.resources.R.string.general_message_sync_paused_low_battery_level),
                onCloseClick = null
            )
        }

        if (syncStalledIssuesState.stalledIssues.isNotEmpty() || syncSolvedIssuesState.solvedIssues.isNotEmpty()) {
            HeaderChips(
                selectedChip = checkedChip,
                stalledIssuesCount = stalledIssuesCount,
                onChipSelected = { checkedChip = it })
        } else {
            checkedChip = SYNC_FOLDERS
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullToRefreshState)
        ) {
            SelectedChipScreen(
                addFolderClicked = addFolderClicked,
                upgradeAccountClicked = onOpenUpgradeAccountClicked,
                stalledIssueDetailsClicked = stalledIssuesDetailsClicked,
                moreClicked = moreClicked,
                issuesInfoClicked = {
                    checkedChip = STALLED_ISSUES
                },
                checkedChip = checkedChip,
                syncStalledIssuesViewModel = syncStalledIssuesViewModel,
                syncFoldersViewModel = syncFoldersViewModel,
                syncSolvedIssuesViewModel = syncSolvedIssuesViewModel,
                syncFoldersState = syncFoldersState
            )
            PullRefreshIndicator(
                modifier = Modifier.align(Alignment.TopCenter),
                refreshing = syncFoldersState.isRefreshing,
                state = pullToRefreshState
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
    ChipBar(modifier = modifier.padding(vertical = 8.dp)) {
        MegaChip(
            selected = selectedChip == SYNC_FOLDERS,
            text = stringResource(id = R.string.sync_folders),
            onClick = { onChipSelected(SYNC_FOLDERS) }
        )
        MegaChip(
            selected = selectedChip == STALLED_ISSUES,
            text = if (stalledIssuesCount > 0) {
                stringResource(R.string.sync_stalled_issues, stalledIssuesCount)
            } else {
                stringResource(id = R.string.sync_stalled_issue_zero)
            },
            onClick = { onChipSelected(STALLED_ISSUES) }
        )
        MegaChip(
            selected = selectedChip == SOLVED_ISSUES,
            text = stringResource(id = sharedR.string.device_center_sync_solved_issues_chip_text),
            onClick = { onChipSelected(SOLVED_ISSUES) }
        )
    }
}

@Composable
private fun SelectedChipScreen(
    addFolderClicked: () -> Unit,
    upgradeAccountClicked: () -> Unit,
    stalledIssueDetailsClicked: (StalledIssueUiItem) -> Unit,
    moreClicked: (StalledIssueUiItem) -> Unit,
    issuesInfoClicked: () -> Unit,
    checkedChip: SyncChip,
    syncFoldersViewModel: SyncFoldersViewModel,
    syncStalledIssuesViewModel: SyncStalledIssuesViewModel,
    syncSolvedIssuesViewModel: SyncSolvedIssuesViewModel,
    syncFoldersState: SyncFoldersState,
) {
    when (checkedChip) {
        SYNC_FOLDERS -> {
            SyncFoldersRoute(
                addFolderClicked = addFolderClicked,
                upgradeAccountClicked = upgradeAccountClicked,
                issuesInfoClicked = issuesInfoClicked,
                viewModel = syncFoldersViewModel,
                state = syncFoldersState
            )
        }

        STALLED_ISSUES -> {
            SyncStalledIssuesRoute(
                stalledIssueDetailsClicked = stalledIssueDetailsClicked,
                moreClicked = moreClicked,
                viewModel = syncStalledIssuesViewModel
            )
        }

        SOLVED_ISSUES -> {
            SyncSolvedIssuesRoute(viewModel = syncSolvedIssuesViewModel)
        }
    }
}
