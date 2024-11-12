package mega.privacy.android.feature.sync.ui.synclist

import mega.privacy.android.shared.resources.R as sharedR
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.R as CoreUiR
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
import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.material.Icon
import androidx.compose.ui.platform.testTag
import mega.privacy.android.feature.sync.ui.synclist.folders.TEST_TAG_SYNC_LIST_SCREEN_FAB
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.banners.ActionBanner
import mega.privacy.android.shared.original.core.ui.controls.banners.WarningBanner
import mega.privacy.android.shared.original.core.ui.controls.buttons.MegaFloatingActionButton
import mega.privacy.android.shared.original.core.ui.controls.buttons.MegaMultiFloatingActionButton
import mega.privacy.android.shared.original.core.ui.controls.buttons.MultiFloatingActionButtonItem
import mega.privacy.android.shared.original.core.ui.controls.buttons.MultiFloatingActionButtonState
import mega.privacy.android.shared.original.core.ui.controls.buttons.rememberMultiFloatingActionButtonState
import mega.privacy.android.shared.original.core.ui.controls.chip.ChipBar
import mega.privacy.android.shared.original.core.ui.controls.chip.MegaChip
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import mega.privacy.android.shared.resources.R as sharedResR
import mega.privacy.android.shared.sync.featuretoggles.SyncFeatures
import mega.privacy.mobile.analytics.event.SyncListBannerUpgradeButtonPressedEvent

@Composable
internal fun SyncListScreen(
    stalledIssuesCount: Int,
    onSyncFolderClicked: () -> Unit,
    onBackupFolderClicked: () -> Unit,
    onAddFolderClicked: () -> Unit,
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

    val scaffoldState = rememberScaffoldState(snackbarHostState = snackBarHostState)

    val syncFoldersState by syncFoldersViewModel.uiState.collectAsStateWithLifecycle()
    val isBackupForAndroidEnabled =
        syncFoldersState.enabledFlags.contains(SyncFeatures.BackupForAndroid)

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
        val multiFabState = rememberMultiFloatingActionButtonState()
        MegaScaffold(
            scaffoldState = scaffoldState,
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
            },
            floatingActionButton = {
                if (isBackupForAndroidEnabled) {
                    when {
                        syncFoldersState.isFreeAccount && syncFoldersState.syncUiItems.isNotEmpty() -> {
                            MegaFloatingActionButton(
                                onClick = onAddFolderClicked,
                                modifier = Modifier.testTag(TEST_TAG_SYNC_LIST_SCREEN_FAB)
                            ) {
                                Icon(
                                    painter = painterResource(CoreUiR.drawable.ic_plus),
                                    contentDescription = null,
                                )
                            }
                        }

                        syncFoldersState.isFreeAccount.not() -> {
                            MegaMultiFloatingActionButton(
                                items = listOf(
                                    MultiFloatingActionButtonItem(
                                        icon = painterResource(id = iconPackR.drawable.ic_sync_01),
                                        label = stringResource(id = R.string.sync_toolbar_title),
                                        onClicked = onSyncFolderClicked,
                                    ),
                                    MultiFloatingActionButtonItem(
                                        icon = painterResource(id = iconPackR.drawable.ic_database),
                                        label = stringResource(id = sharedResR.string.sync_add_new_backup_toolbar_title),
                                        onClicked = onBackupFolderClicked,
                                    ),
                                ),
                                modifier = Modifier.testTag(TEST_TAG_SYNC_LIST_SCREEN_FAB),
                                multiFabState = multiFabState,
                                onStateChanged = { state -> multiFabState.value = state }
                            )
                        }
                    }
                } else {
                    if (syncFoldersState.syncUiItems.isNotEmpty() || syncFoldersState.isLoading) {
                        MegaFloatingActionButton(
                            onClick = onSyncFolderClicked,
                            modifier = Modifier.testTag(TEST_TAG_SYNC_LIST_SCREEN_FAB)
                        ) {
                            Icon(
                                painter = painterResource(CoreUiR.drawable.ic_plus),
                                contentDescription = stringResource(id = sharedResR.string.device_center_sync_add_new_syn_button_option),
                            )
                        }
                    }
                }
            },
            blurContent = if (isBackupForAndroidEnabled && multiFabState.value == MultiFloatingActionButtonState.EXPANDED) { ->
                multiFabState.value = MultiFloatingActionButtonState.COLLAPSED
            } else {
                null
            },
            content = { paddingValues ->
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
                    addFolderClicked = onSyncFolderClicked,
                    syncPermissionsManager = syncPermissionsManager,
                    onOpenUpgradeAccountClicked = onOpenUpgradeAccountClicked,
                    syncFoldersViewModel = syncFoldersViewModel,
                    syncStalledIssuesViewModel = syncStalledIssuesViewModel,
                    syncSolvedIssuesViewModel = syncSolvedIssuesViewModel,
                    deviceName = title ?: "",
                    selectedChip = selectedChip,
                    snackBarHostState = scaffoldState.snackbarHostState,
                )
            },
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SyncListScreenContent(
    modifier: Modifier,
    snackBarHostState: SnackbarHostState,
    stalledIssuesCount: Int,
    stalledIssuesDetailsClicked: (StalledIssueUiItem) -> Unit,
    moreClicked: (StalledIssueUiItem) -> Unit,
    addFolderClicked: () -> Unit,
    syncPermissionsManager: SyncPermissionsManager,
    onOpenUpgradeAccountClicked: () -> Unit,
    syncFoldersViewModel: SyncFoldersViewModel,
    syncStalledIssuesViewModel: SyncStalledIssuesViewModel,
    syncSolvedIssuesViewModel: SyncSolvedIssuesViewModel,
    deviceName: String,
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

        if (checkedChip != SYNC_FOLDERS || syncStalledIssuesState.stalledIssues.isNotEmpty() || syncSolvedIssuesState.solvedIssues.isNotEmpty()) {
            HeaderChips(
                selectedChip = checkedChip,
                stalledIssuesCount = stalledIssuesCount,
                onChipSelected = { checkedChip = it })
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
                syncFoldersState = syncFoldersState,
                snackBarHostState = snackBarHostState,
                deviceName = deviceName,
                isBackupForAndroidEnabled = syncFoldersState.enabledFlags.contains(SyncFeatures.BackupForAndroid),
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
            text = stringResource(id = R.string.sync_folders)
        ) { onChipSelected(SYNC_FOLDERS) }
        MegaChip(
            selected = selectedChip == STALLED_ISSUES,
            text = if (stalledIssuesCount > 0) {
                stringResource(R.string.sync_stalled_issues, stalledIssuesCount)
            } else {
                stringResource(id = R.string.sync_stalled_issue_zero)
            }
        ) { onChipSelected(STALLED_ISSUES) }
        MegaChip(
            selected = selectedChip == SOLVED_ISSUES,
            text = stringResource(id = sharedR.string.device_center_sync_solved_issues_chip_text)
        ) { onChipSelected(SOLVED_ISSUES) }
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
    snackBarHostState: SnackbarHostState,
    deviceName: String,
    isBackupForAndroidEnabled: Boolean,
) {
    when (checkedChip) {
        SYNC_FOLDERS -> {
            SyncFoldersRoute(
                addFolderClicked = addFolderClicked,
                upgradeAccountClicked = upgradeAccountClicked,
                issuesInfoClicked = issuesInfoClicked,
                viewModel = syncFoldersViewModel,
                state = syncFoldersState,
                snackBarHostState = snackBarHostState,
                deviceName = deviceName,
                isBackupForAndroidEnabled = isBackupForAndroidEnabled,
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
