package mega.privacy.android.feature.sync.ui.synclist

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.controls.appbar.AppBarType
import mega.privacy.android.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
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
import mega.privacy.android.legacy.core.ui.controls.chips.PhotoChip

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
        scrimColor = Color.Black.copy(alpha = 0.32f),
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
                    title = stringResource(R.string.sync_toolbar_title),
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
                    syncPermissionsManager = syncPermissionsManager
                )
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = snackBarHostState,
                    snackbar = { data ->
                        Snackbar(
                            snackbarData = data,
                            modifier = Modifier.padding(bottom = 4.dp),
                            backgroundColor = MaterialTheme.colors.onPrimary,
                        )
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
    syncFoldersViewModel: SyncFoldersViewModel = hiltViewModel(),
    syncStalledIssuesViewModel: SyncStalledIssuesViewModel = hiltViewModel(),
    syncSolvedIssuesViewModel: SyncSolvedIssuesViewModel = hiltViewModel(),
) {
    var checkedChip by rememberSaveable { mutableStateOf(SYNC_FOLDERS) }

    val syncFoldersState by syncFoldersViewModel.state.collectAsStateWithLifecycle()

    val pullToRefreshState = rememberPullRefreshState(
        refreshing = syncFoldersState.isRefreshing,
        onRefresh = {
            syncFoldersViewModel.onSyncRefresh()
        })

    Column(modifier) {
        SyncPermissionWarningBanner(
            syncPermissionsManager = syncPermissionsManager
        )
        HeaderChips(
            selectedChip = checkedChip,
            stalledIssuesCount = stalledIssuesCount,
            onChipSelected = { checkedChip = it })
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullToRefreshState)
        ) {
            SelectedChipScreen(
                addFolderClicked = addFolderClicked,
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
    Row(modifier.padding(16.dp)) {
        PhotoChip(
            text = stringResource(id = R.string.sync_folders),
            onClick = { onChipSelected(SYNC_FOLDERS) },
            isChecked = selectedChip == SYNC_FOLDERS
        )
        PhotoChip(
            text = if (stalledIssuesCount > 0) stringResource(
                R.string.sync_stalled_issues,
                stalledIssuesCount
            ) else stringResource(id = R.string.sync_stalled_issue_zero),
            onClick = { onChipSelected(STALLED_ISSUES) },
            Modifier.padding(horizontal = 8.dp),
            isChecked = selectedChip == STALLED_ISSUES,
        )
        PhotoChip(
            text = stringResource(id = R.string.sync_solved_issues),
            onClick = { onChipSelected(SOLVED_ISSUES) },
            isChecked = selectedChip == SOLVED_ISSUES,
        )
    }
}

@Composable
private fun SelectedChipScreen(
    addFolderClicked: () -> Unit,
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

@CombinedThemePreviews
@Composable
private fun SyncListScreenPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        SyncListScreen(
            stalledIssuesCount = 3,
            addFolderClicked = {},
            actionSelected = { _, _ -> },
            snackBarHostState = SnackbarHostState(),
            syncPermissionsManager = SyncPermissionsManager(LocalContext.current),
            actions = listOf(),
            onActionPressed = {
            }
        )
    }
}