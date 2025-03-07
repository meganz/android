package mega.privacy.android.feature.sync.ui.synclist

import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.shared.resources.R as sharedResR
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.ScaffoldDefaults
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersUiState
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersViewModel
import mega.privacy.android.feature.sync.ui.synclist.folders.TEST_TAG_SYNC_LIST_SCREEN_FAB
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
import mega.privacy.android.shared.original.core.ui.utils.ComposableLifecycle
import mega.privacy.mobile.analytics.event.AndroidBackupFABButtonPressedEvent
import mega.privacy.mobile.analytics.event.AndroidSyncMultiFABButtonPressedEvent

@Composable
internal fun SyncListScreen(
    stalledIssuesCount: Int,
    onSyncFolderClicked: () -> Unit,
    onBackupFolderClicked: () -> Unit,
    onOpenMegaFolderClicked: (handle: Long) -> Unit,
    onCameraUploadsSettingsClicked: () -> Unit,
    actionSelected: (item: StalledIssueUiItem, selectedAction: StalledIssueResolutionAction) -> Unit,
    snackBarHostState: SnackbarHostState,
    syncPermissionsManager: SyncPermissionsManager,
    actions: List<MenuAction>,
    onActionPressed: (MenuAction) -> Unit,
    onSelectStopBackupDestinationClicked: () -> Unit,
    onOpenUpgradeAccountClicked: () -> Unit,
    syncFoldersViewModel: SyncFoldersViewModel,
    syncStalledIssuesViewModel: SyncStalledIssuesViewModel,
    syncSolvedIssuesViewModel: SyncSolvedIssuesViewModel,
    title: String,
    isInCloudDrive: Boolean = false,
    selectedChip: SyncChip = SYNC_FOLDERS,
    onFabExpanded: (Boolean) -> Unit = {},
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
        var isWarningBannerDisplayed by rememberSaveable { mutableStateOf(false) }
        ComposableLifecycle { event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isWarningBannerDisplayed =
                    syncPermissionsManager.isManageExternalStoragePermissionGranted().not()
                            || syncPermissionsManager.isDisableBatteryOptimizationGranted().not()
            }
        }

        val multiFabState = rememberMultiFloatingActionButtonState()

        DisposableEffect(multiFabState.value) {
            onFabExpanded(multiFabState.value == MultiFloatingActionButtonState.EXPANDED)
            onDispose { }
        }

        MegaScaffold(
            scaffoldState = scaffoldState,
            contentWindowInsets = if (isInCloudDrive) WindowInsets(0.dp) else ScaffoldDefaults.contentWindowInsets,
            topBar = {
                if (!isInCloudDrive) {
                    MegaAppBar(
                        title = title.ifEmpty { stringResource(R.string.sync_toolbar_title) },
                        appBarType = AppBarType.BACK_NAVIGATION,
                        onNavigationPressed = {
                            onBackPressedDispatcher?.onBackPressed()
                        },
                        actions = actions,
                        onActionPressed = onActionPressed,
                        elevation = if (isWarningBannerDisplayed || syncFoldersState.isWarningBannerDisplayed) AppBarDefaults.TopAppBarElevation else 0.dp,
                        windowInsets = WindowInsets(0.dp),
                    )
                }
            },
            floatingActionButton = {
                if (syncFoldersState.syncUiItems.isNotEmpty() || syncFoldersState.isLoading) {
                    MegaMultiFloatingActionButton(
                        items = listOf(
                            MultiFloatingActionButtonItem(
                                icon = painterResource(id = iconPackR.drawable.ic_sync_01),
                                label = stringResource(id = R.string.sync_toolbar_title),
                                onClicked = {
                                    onSyncFolderClicked()
                                    multiFabState.value = MultiFloatingActionButtonState.COLLAPSED
                                },
                            ),
                            MultiFloatingActionButtonItem(
                                icon = painterResource(id = iconPackR.drawable.ic_database),
                                label = stringResource(id = sharedResR.string.sync_add_new_backup_toolbar_title),
                                onClicked = {
                                    Analytics.tracker.trackEvent(
                                        AndroidBackupFABButtonPressedEvent
                                    )
                                    onBackupFolderClicked()
                                    multiFabState.value = MultiFloatingActionButtonState.COLLAPSED
                                },
                            ),
                        ),
                        modifier = Modifier.testTag(TEST_TAG_SYNC_LIST_SCREEN_FAB),
                        multiFabState = multiFabState,
                        onStateChanged = { state ->
                            if (state == MultiFloatingActionButtonState.EXPANDED) {
                                Analytics.tracker.trackEvent(
                                    AndroidSyncMultiFABButtonPressedEvent
                                )
                            }
                            onFabExpanded(state == MultiFloatingActionButtonState.EXPANDED)
                            multiFabState.value = state
                        }
                    )
                }
            },
            blurContent = if (multiFabState.value == MultiFloatingActionButtonState.EXPANDED) { ->
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
                    deviceName = title,
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
    onAddNewSyncClicked: () -> Unit,
    onAddNewBackupClicked: () -> Unit,
    onOpenMegaFolderClicked: (handle: Long) -> Unit,
    onCameraUploadsSettingsClicked: () -> Unit,
    syncPermissionsManager: SyncPermissionsManager,
    onSelectStopBackupDestinationClicked: () -> Unit,
    onOpenUpgradeAccountClicked: () -> Unit,
    syncFoldersViewModel: SyncFoldersViewModel,
    syncStalledIssuesViewModel: SyncStalledIssuesViewModel,
    syncSolvedIssuesViewModel: SyncSolvedIssuesViewModel,
    deviceName: String,
    selectedChip: SyncChip = SYNC_FOLDERS,
) {
    var checkedChip by rememberSaveable { mutableStateOf(selectedChip) }

    val syncFoldersUiState by syncFoldersViewModel.uiState.collectAsStateWithLifecycle()
    val syncStalledIssuesState by syncStalledIssuesViewModel.state.collectAsStateWithLifecycle()
    val syncSolvedIssuesState by syncSolvedIssuesViewModel.state.collectAsStateWithLifecycle()

    val pullToRefreshState = rememberPullRefreshState(
        refreshing = syncFoldersUiState.isRefreshing,
        onRefresh = {
            syncFoldersViewModel.onSyncRefresh()
        })

    Column(modifier) {
        SyncPermissionWarningBanner(
            syncPermissionsManager = syncPermissionsManager
        )
        if (syncFoldersUiState.isStorageOverQuota) {
            ActionBanner(
                mainText = stringResource(sharedR.string.sync_error_storage_over_quota_banner_title),
                leftActionText = stringResource(sharedR.string.general_upgrade_button),
                leftActionClicked = onOpenUpgradeAccountClicked,
                modifier = Modifier.padding(top = 20.dp)
            )
            MegaDivider(
                dividerType = DividerType.FullSize,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        } else if (syncFoldersUiState.syncUiItems.isNotEmpty() && syncFoldersUiState.isLowBatteryLevel) {
            WarningBanner(
                textString = stringResource(id = sharedResR.string.general_message_sync_paused_low_battery_level),
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
                onAddNewSyncClicked = onAddNewSyncClicked,
                onAddNewBackupClicked = onAddNewBackupClicked,
                onSelectStopBackupDestinationClicked = onSelectStopBackupDestinationClicked,
                stalledIssueDetailsClicked = stalledIssuesDetailsClicked,
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
                snackBarHostState = snackBarHostState,
                deviceName = deviceName,
            )
            PullRefreshIndicator(
                modifier = Modifier.align(Alignment.TopCenter),
                refreshing = syncFoldersUiState.isRefreshing,
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
    onAddNewSyncClicked: () -> Unit,
    onAddNewBackupClicked: () -> Unit,
    onSelectStopBackupDestinationClicked: () -> Unit,
    onOpenMegaFolderClicked: (handle: Long) -> Unit,
    onCameraUploadsSettingsClicked: () -> Unit,
    stalledIssueDetailsClicked: (StalledIssueUiItem) -> Unit,
    moreClicked: (StalledIssueUiItem) -> Unit,
    issuesInfoClicked: () -> Unit,
    checkedChip: SyncChip,
    syncFoldersViewModel: SyncFoldersViewModel,
    syncStalledIssuesViewModel: SyncStalledIssuesViewModel,
    syncSolvedIssuesViewModel: SyncSolvedIssuesViewModel,
    syncFoldersUiState: SyncFoldersUiState,
    snackBarHostState: SnackbarHostState,
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
                snackBarHostState = snackBarHostState,
                deviceName = deviceName,
                onOpenMegaFolderClicked = onOpenMegaFolderClicked,
                onCameraUploadsSettingsClicked = onCameraUploadsSettingsClicked,
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
